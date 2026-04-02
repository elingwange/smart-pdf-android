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
import kotlinx.coroutines.yield
import java.io.File

object PdfScanner {
    private const val TAG = "---ELog"

    /**
     * 混合扫描入口：利用 Flow 实现响应式加载
     */
    fun getPdfsFlow(context: Context): Flow<List<PdfFile>> = flow {
        val startTime = System.currentTimeMillis()
        val packageName = context.packageName

        // 第一阶段：快速获取系统索引 (MediaStore)
        // 增加 File.exists() 校验，过滤掉 MediaStore 中的“僵尸”缓存路径
        val mediaStorePdfs = scanMediaStore(context)
        val validMediaStorePdfs = mediaStorePdfs.filter {
            val file = File(it.path)
            file.exists() && file.length() > 0 && !it.path.contains(packageName, ignoreCase = true)
        }

        Log.d(TAG, "MediaStore found ${validMediaStorePdfs.size} valid files")
        val count = validMediaStorePdfs.count { it.name.contains("incoming") }
        Log.d(TAG, "MediaStore found $count cache files")
        //validMediaStorePdfs.map { it.name }.forEach { Log.d(TAG, it) }

        emit(validMediaStorePdfs)

        // 第二阶段：BFS 物理扫描补漏
        // 目标：发现 MediaStore 还没来得及索引的新文件
        val root = Environment.getExternalStorageDirectory() ?: return@flow
        val queue = ArrayDeque<File>()
        queue.add(root)

        val batchList = mutableListOf<PdfFile>()
        val knownPaths = validMediaStorePdfs.map { it.path }.toSet()

        while (queue.isNotEmpty()) {
            val currentDir = queue.removeFirst()

            currentCoroutineContext().ensureActive()
            yield()

            val files = currentDir.listFiles() ?: continue
            for (file in files) {
                if (file.isDirectory) {
                    // ✨ 核心修复：改进过滤逻辑，排除 data, cache 和私有包名目录
                    if (!shouldSkip(file, packageName)) {
                        queue.addLast(file)
                    }
                } else if (file.extension.equals("pdf", ignoreCase = true)) {
                    val absolutePath = file.absolutePath
                    // 只有 MediaStore 没记录的文件才通过物理扫描添加
                    if (!knownPaths.contains(absolutePath) && !absolutePath.contains(
                            packageName,
                            ignoreCase = true
                        )
                    ) {
                        batchList.add(file.toPdfFile())

                        if (batchList.size >= 15) {
                            emit(batchList.toList())
                            batchList.clear()
                        }
                    }
                }
            }
        }


        if (batchList.isNotEmpty()) emit(batchList)
        Log.d(TAG, "Scan completed in ${System.currentTimeMillis() - startTime}ms")
    }.flowOn(Dispatchers.IO)

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

        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ?"
        val selectionArgs = arrayOf("application/pdf")
        val sortOrder = "${MediaStore.Files.FileColumns.DATE_MODIFIED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )
                ?.use { cursor ->
                    val nameCol =
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                    val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                    val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                    val dateCol =
                        cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_MODIFIED)

                    while (cursor.moveToNext()) {
                        val path = cursor.getString(pathCol) ?: continue
                        pdfs.add(
                            PdfFile(
                                name = cursor.getString(nameCol) ?: File(path).name,
                                path = path,
                                size = cursor.getLong(sizeCol),
                                lastModified = cursor.getLong(dateCol) * 1000
                            )
                        )
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "MediaStore query error", e)
        }
        return pdfs
    }

    /**
     * ✨ 关键修复：严密的目录过滤逻辑
     */
    private fun shouldSkip(dir: File, packageName: String): Boolean {
        val path = dir.absolutePath.lowercase()
        val name = dir.name.lowercase()

        return name.startsWith(".") ||
                name == "android" ||
                name == "data" ||
                name == "cache" ||
                path.contains("/$packageName/") || // 排除本 App 产生的任何目录
                path.contains("/temp/") ||
                path.contains("/com.") // 排除其他 App 的私有数据区
    }

    private fun File.toPdfFile() = PdfFile(
        name = this.name,
        path = this.absolutePath,
        size = this.length(),
        lastModified = this.lastModified()
    )
}