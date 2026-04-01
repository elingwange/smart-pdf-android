package com.quantumstudio.smartpdf.data.repository

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import com.quantumstudio.smartpdf.data.local.PdfFileDao
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.data.scanner.PdfScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.Thread.yield

class PdfRepository(
    private val pdfFileDao: PdfFileDao
) {

    // 维度 6：数据一致性。UI 只观察这个 Flow，不关心数据是怎么来的。
    fun getAllPdfsFlow(context: Context): Flow<List<PdfFile>> =
        pdfFileDao.getAllPdfsFlow().map { list ->
            // 只要数据库里的数据一变，立刻在这里进行过滤
            list.emergencyFilter(context)
        }

    // 2. 将 emergencyFilter 设为私有或保留，供内部使用
    private fun List<PdfFile>.emergencyFilter(context: Context): List<PdfFile> {
        val packageName = context.packageName
        return this.filter { pdf ->
            // 这里保留你之前的逻辑
            val path = pdf.path.lowercase()
            val file = File(pdf.path)

            // 物理存在 + 路径合法性
            val exists = file.exists() && file.length() > 0
            val isNotCache = !path.contains("/cache/") && !path.contains("/$packageName/")
            val isNotData = !path.contains("/data/user/") && !path.contains("/android/data/")

            exists && isNotCache && isNotData
        }
    }

    /**
     * 核心同步逻辑：不再返回结果，只负责“生产”数据入库
     */
    suspend fun syncPdfFiles(context: Context) = withContext(Dispatchers.IO) {
        // 1. 调用我们之前写的“混合扫描器”，获取 Flow
        // 这样扫描到一个，数据库存一个，UI 就能出来一个
        PdfScanner.getPdfsFlow(context).collect { batch ->
            if (batch.isNotEmpty()) {
                Log.d("---ELog", "Insert ${batch.size} pdf files")
                pdfFileDao.insertAll(batch)
            }
        }
        // 2. 扫描物理文件完成后，再处理页数补全
        // 注意：这里不再开启新的独立 CoroutineScope，而是直接在当前作用域顺序执行
        complementMissingPageCounts()
    }

    private suspend fun complementMissingPageCounts() {
        val missingPdfs = pdfFileDao.getFilesWithNoPages() // 建议 Dao 增加专门查 0 页的接口
        Log.d("---ELog", "missingPdfs-> ${missingPdfs.size} files")

        // 维度 7：性能边界。为了防止 TCL 603 瞬间打开几十个 PDF 导致 OOM
        // 我们顺序处理，或者限制并发数为 2
        withContext(Dispatchers.IO) {
            missingPdfs.forEach { pdf ->
                val file = File(pdf.path)
                if (file.exists()) {
                    val realCount = getPdfPageCount(file)
                    // ✨ 关键优化：只有获取到了真实页数才更新，避免死循环
                    if (realCount > 0) {
                        pdfFileDao.updatePageCount(pdf.path, realCount)
                    } else {
                        // 如果文件损坏或打不开，给它一个特殊值（如 -1），防止它一直占着 getFilesWithNoPages 的名额
                        pdfFileDao.updatePageCount(pdf.path, -1)
                    }
                } else {
                    // 文件不存在了，直接标记或删除
                    pdfFileDao.deleteByPath(pdf.path)
                }
                yield() // 保持 UI 响应
            }
        }
    }


    // 切换收藏状态
    suspend fun toggleFavorite(path: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        // 假设你的 PdfFileDao 中有 updateFavorite 方法，或者直接 update 整个对象
        pdfFileDao.updateFavorite(path, isFavorite)
    }

    suspend fun markAsRead(path: String) = withContext(Dispatchers.IO) {
        pdfFileDao.updateLastReadTime(path, System.currentTimeMillis())
    }

    suspend fun deletePdfFile(pdf: PdfFile): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = File(pdf.path)
            // 1. 执行物理删除
            val deleted = if (file.exists()) file.delete() else true

            if (deleted) {
                // 2. 从数据库删除记录
                pdfFileDao.deleteByPath(pdf.path)
            }
            deleted
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    suspend fun renamePdfFile(pdf: PdfFile, newName: String): PdfFile? =
        withContext(Dispatchers.IO) {
            try {
                val oldFile = File(pdf.path)
                val newPath = oldFile.parent + File.separator + newName
                val newFile = File(newPath)

                if (oldFile.renameTo(newFile)) {
                    // ✨ 数据库处理：因为 path 是主键，必须先删后插
                    val newPdf = pdf.copy(path = newPath, name = newName)
                    pdfFileDao.deleteByPath(pdf.path)
                    pdfFileDao.insertAll(listOf(newPdf))
                    newPdf
                } else null
            } catch (e: Exception) {
                null
            }
        }

    // ✨ 新增：更新阅读进度（页码 + 时间戳）
    suspend fun updateProgress(path: String, page: Int) {
        val timestamp = System.currentTimeMillis()
        pdfFileDao.updatePageProgress(path, page, timestamp)
    }

    suspend fun getOrInsertPdf(path: String): PdfFile? = withContext(Dispatchers.IO) {
        // 1. 先按路径查
        var pdf = pdfFileDao.getPdfByPath(path)

        if (pdf == null) {
            val file = File(path)
            if (file.exists()) {
                val pageCount = getPdfPageCount(file)

                // ✨ 战地急救逻辑：针对 incoming 临时文件，尝试通过文件名或大小匹配已有记录
                // 这样即使路径变了，阅读进度也有机会接上（可选优化）

                pdf = PdfFile(
                    path = path, // 可能是真实路径，也可能是 cache 路径
                    name = file.name,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    currentPage = 0,
                    lastReadTime = System.currentTimeMillis(),
                    pages = pageCount
                )

                // 2. 插入数据库
                // 注意：如果 path 是主键，且由于 emergencyFilter 导致它之前被“隐藏”了，
                // 这里的插入会确保 Reader 界面能拿到实体对象。
                pdfFileDao.insertAll(listOf(pdf))

                // 3. 重新获取一次，确保拿到数据库分配的默认值
                pdf = pdfFileDao.getPdfByPath(path)
            }
        }
        pdf
    }

    // 辅助函数：通过原生 Renderer 获取页数
    private fun getPdfPageCount(file: File): Int {
        return try {
            val pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            val renderer = PdfRenderer(pfd)
            val count = renderer.pageCount
            renderer.close()
            pfd.close()
            count
        } catch (e: Exception) {
            0 // 如果读取失败，默认返回 0
        }
    }

}