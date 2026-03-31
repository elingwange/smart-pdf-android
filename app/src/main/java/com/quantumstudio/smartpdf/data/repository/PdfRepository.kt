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
import kotlinx.coroutines.withContext
import java.io.File
import java.lang.Thread.yield

class PdfRepository(
    private val pdfFileDao: PdfFileDao
) {

    // 维度 6：数据一致性。UI 只观察这个 Flow，不关心数据是怎么来的。
    fun getAllPdfsFlow(): Flow<List<PdfFile>> = pdfFileDao.getAllPdfsFlow()

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
        //       complementMissingPageCounts()
    }

    private suspend fun complementMissingPageCounts() {
        val missingPdfs = pdfFileDao.getFilesWithNoPages() // 建议 Dao 增加专门查 0 页的接口

        // 维度 7：性能边界。为了防止 TCL 603 瞬间打开几十个 PDF 导致 OOM
        // 我们顺序处理，或者限制并发数为 2
        missingPdfs.forEach { pdf ->
            val file = File(pdf.path)
            if (file.exists()) {
                val realCount = getPdfPageCount(file)
                if (realCount > 0) {
                    pdfFileDao.updatePageCount(pdf.path, realCount)
                }
            }
            // 每处理一个 PDF 释放一次 CPU，防止长时间霸占 IO 导致 UI 卡顿
            yield()
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

    // ✨ 核心补全：确保数据库里一定有这个文件
    suspend fun getOrInsertPdf(path: String): PdfFile? = withContext(Dispatchers.IO) {
        // 1. 先查数据库
        var pdf = pdfFileDao.getPdfByPath(path)

        if (pdf == null) {
            val file = File(path)
            if (file.exists()) {
                // 2. 获取 PDF 真实页数 (关键步骤)
                val pageCount = getPdfPageCount(file)

                pdf = PdfFile(
                    path = path,
                    name = file.name,
                    size = file.length(),
                    lastModified = file.lastModified(),
                    currentPage = 0,
                    lastReadTime = System.currentTimeMillis(),
                    pages = pageCount
                )
                // 3. 插入数据库
                pdfFileDao.insertAll(listOf(pdf))
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