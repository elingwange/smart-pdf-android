package com.quantumstudio.smartpdf.data.scanner

import android.app.Application
import android.os.Environment
import android.util.Log
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.data.scanner.PdfScanner.TAG
import com.quantumstudio.smartpdf.data.scanner.PdfScanner.scanMediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import javax.inject.Inject

class PdfScanner2 @Inject constructor(private val context: Application) {

    // 原子逻辑 1：纯系统数据库查询
    suspend fun fetchMediaStoreIndex(): List<PdfFile> = withContext(
        Dispatchers.IO
    ) {

        val startTime = System.currentTimeMillis()

        var mediaStorePdfs = scanMediaStore(context)
        Log.d(TAG, "MediaStore found ${mediaStorePdfs.size} valid files")
        //同步延迟，给它一次机会
        if (mediaStorePdfs.isEmpty()) {
            Log.d("---ELog", "首次查询为空，尝试延迟重试...")
            delay(300)
            mediaStorePdfs = PdfScanner.scanMediaStore(context)
        }

        val end = System.currentTimeMillis()
        Log.d("---ELog", "查询数据耗时: ${end - startTime} ms")

        Log.d(TAG, "MediaStore found ${mediaStorePdfs.size} valid files")

        mediaStorePdfs
    }

    // 原子逻辑 2：物理文件过滤（耗时 IO）
    suspend fun List<PdfFile>.filterExisting(): List<PdfFile> = withContext(Dispatchers.IO) {
        this@filterExisting.filter { File(it.path).exists() }
    }

    /**
     * 深度扫描：由于不依赖系统数据库，直接遍历文件树
     * 价值：能找回 MediaStore 漏掉的、隐藏目录下的 PDF
     */
    fun scanFullStorage() = flow<List<PdfFile>> {
        val root = Environment.getExternalStorageDirectory()
        // 初始安全检查：根目录不可读则直接退出
        if (root == null || !root.canRead()) return@flow

        val queue = ArrayDeque<File>().apply { add(root) }
        val batchSize = 15
        val currentBatch = mutableListOf<PdfFile>()
        val selfPkg = context.packageName // 提前提取包名，避免循环内重复调用 context

        while (queue.isNotEmpty()) {
            val currentDir = queue.removeFirst()

            // yield 包含了检查取消和释放 CPU 的双重作用
            yield()

            val files = currentDir.listFiles() ?: continue

            for (file in files) {
                if (file.isDirectory) {
                    // 入队前过滤：只让合法的文件夹进队列
                    if (!shouldSkipDirectory(file)) {
                        queue.addLast(file)
                    }
                } else if (file.extension.equals("pdf", ignoreCase = true)) {
                    // 过滤 1：排除自身包名路径
                    if (file.path.contains(selfPkg)) continue

                    currentBatch.add(file.toPdfFile())

                    // 批量交付
                    if (currentBatch.size >= batchSize) {
                        // 使用 ArrayList 构造函数比 toList() 快，因为它减少了额外的类型转换检查
                        emit(ArrayList(currentBatch))
                        currentBatch.clear()
                    }
                }
            }
        }

        if (currentBatch.isNotEmpty()) {
            emit(ArrayList(currentBatch))
        }
    }.flowOn(Dispatchers.IO)

    // 放在类成员位置，只创建一次
    private val BLACK_LIST = setOf("android", "data", "obb", "cache", "temp", "tmp")

    /**
     * 判断是否应当跳过该目录
     * 逻辑：排除隐藏目录、系统敏感目录、以及已知的缓存/临时文件夹
     */
    private fun shouldSkipDirectory(file: File): Boolean {
        // 1. 最快：内存字符串匹配 (O(1))
        val name = file.name
        if (name.startsWith(".")) return true
        if (BLACK_LIST.contains(name.lowercase())) return true

        // 2. 较慢：访问元数据 (System Call)
        // 注意：在 scanStorage 循环里，currentDir 已经是文件夹了，
        // 所以 isDirectory 其实可以省略，除非你担心路径发生了动态变化。
        if (!file.canRead()) return true

        return false
    }

    private fun File.toPdfFile(): PdfFile {
        return PdfFile(
            path = this.absolutePath,
            name = this.name,
            size = this.length(),            // 对应 Long 类型字段
            lastModified = this.lastModified(),      // 对应 Long 类型字段
        )
    }
}