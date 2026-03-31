package com.quantumstudio.smartpdf.data.scanner

import android.content.Context
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.quantumstudio.smartpdf.data.model.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.io.File
import java.lang.Thread.yield

object PdfScanner {

    /**
     * 混合扫描入口：利用 Flow 实现响应式加载
     * 1. 立即发射 MediaStore 索引结果（求快）
     * 2. 异步执行 BFS 物理扫描补漏（求全）
     */
    fun getPdfsFlow(context: Context): Flow<List<PdfFile>> = flow {
        val startTime = System.currentTimeMillis()
        Log.d("---ELog", "Scan start...")
        // 第一阶段：快速获取系统索引 (MediaStore)
        val mediaStorePdfs = scanMediaStore(context)
        Log.d("---ELog", "${mediaStorePdfs.size} pdf files found in MediaStore")
        emit(mediaStorePdfs) // 毫秒级返回，UI 立即显示

        // 第二阶段：物理扫描补漏-BFS
        val root = Environment.getExternalStorageDirectory() ?: return@flow
        val queue = ArrayDeque<File>()
        queue.add(root)

        val batchList = mutableListOf<PdfFile>()
        val knownPaths = mediaStorePdfs.map { it.path }.toSet()

        while (queue.isNotEmpty()) {
            val currentDir = queue.removeFirst()

            // 维度 4：生命周期感知。如果协程被取消（如用户退出界面），立即停止
            currentCoroutineContext().ensureActive()

            // 维度 7：性能边界。让出 CPU，给 UI 刷新留出时间
            yield()

            val files = currentDir.listFiles() ?: continue
            for (file in files) {
                if (file.isDirectory) {
                    // 过滤掉敏感或无意义目录，减少无效 I/O
                    if (!shouldSkip(file)) queue.addLast(file)
                } else if (file.extension.equals("pdf", ignoreCase = true)) {
                    if (!knownPaths.contains(file.absolutePath)) {
                        batchList.add(file.toPdfFile())

                        // 每发现 10 个新文件更新一次 UI，避免频繁回调导致掉帧
                        if (batchList.size >= 10) {
                            emit(batchList.toList())
                            batchList.clear()
                        }
                    }
                }
            }
        }
        if (batchList.isNotEmpty()) emit(batchList)
        val endTime = System.currentTimeMillis()
        Log.d("---ELog", "Scan end")
        Log.d("---ELog", "Scan time: ${endTime - startTime}ms")
    }.flowOn(Dispatchers.IO) // 强制在 IO 线程池执行

    /**
     * 利用 ContentResolver 查询 MediaStore 索引
     */
    private fun scanMediaStore(context: Context): List<PdfFile> {
        val pdfs = mutableListOf<PdfFile>()
        val collection = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_MODIFIED
        )
        // 过滤 PDF 类型
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("application/pdf")

        context.contentResolver.query(collection, projection, selection, selectionArgs, null)
            ?.use { cursor ->
                val nameCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol =
                    cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                while (cursor.moveToNext()) {
                    pdfs.add(
                        PdfFile(
                            name = cursor.getString(nameCol),
                            path = cursor.getString(pathCol),
                            size = cursor.getLong(sizeCol),
                            lastModified = cursor.getLong(dateCol) * 1000 // 转为毫秒
                        )
                    )
                }
            }
        return pdfs
    }

    private fun shouldSkip(dir: File): Boolean {
        val name = dir.name
        return name.startsWith(".") || name == "Android" || name == "Data"
    }

    private fun File.toPdfFile() = PdfFile(
        name = this.name,
        path = this.absolutePath,
        size = this.length(),
        lastModified = this.lastModified()
    )


    /**
     * 使用 BFS（广度优先搜索）迭代扫描所有 PDF 文件
     * 解决了递归带来的栈溢出风险和高频栈帧创建开销
     */
    fun scanAllPdfFiles(context: Context): List<PdfFile> {
        val pdfList = mutableListOf<PdfFile>()
        val root = Environment.getExternalStorageDirectory() ?: return pdfList

        // 使用队列存放待扫描的文件夹
        val queue = ArrayDeque<File>()
        queue.add(root)

        while (queue.isNotEmpty()) {
            // 取出当前层级的文件夹
            val currentDir = queue.removeFirst()

            // 每处理一个文件夹，主动释放一次 CPU 占用权
            // 如果没有其他更高优先级的任务，它会立即恢复执行
            // 如果 UI 线程在抢资源，它会礼貌地让路
            yield()

            // listFiles() 是 I/O 操作，建议在外层配合协程使用 Dispatchers.IO
            val files = currentDir.listFiles() ?: continue

            for (file in files) {
                if (file.isDirectory) {
                    // 发现文件夹，不递归，直接入队等待后续处理
                    queue.addLast(file)
                } else if (file.isFile && file.extension.equals("pdf", ignoreCase = true)) {
                    // 发现 PDF，直接封装
                    pdfList.add(
                        PdfFile(
                            name = file.name,
                            path = file.absolutePath,
                            size = file.length(),
                            pages = 0,
                            lastModified = file.lastModified()
                        )
                    )
                }
            }
        }
        return pdfList
    }


    /**
     * 递归扫描目录
     */
    private fun scanDirectory(dir: File, pdfList: MutableList<PdfFile>) {
        if (!dir.exists() || !dir.isDirectory) return

        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.isDirectory) {
                scanDirectory(file, pdfList)
            } else if (file.isFile && file.extension.lowercase() == "pdf") {
                // 封装成 PdfFile
                val pdfFile = PdfFile(
                    name = file.name,
                    path = file.absolutePath,
                    size = file.length(),
                    pages = 0, // 后面可用 PDF 库解析页数
                    lastModified = file.lastModified()
                )
                pdfList.add(pdfFile)
            }
        }
    }
}
