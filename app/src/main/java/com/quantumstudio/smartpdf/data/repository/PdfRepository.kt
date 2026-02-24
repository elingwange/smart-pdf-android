package com.quantumstudio.smartpdf.data.repository

import android.content.Context
import com.quantumstudio.smartpdf.data.local.PdfFileDao
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.data.scanner.PdfScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class PdfRepository(
    private val pdfFileDao: PdfFileDao
) {

    suspend fun getAllPdfs(context: Context): List<PdfFile> = withContext(Dispatchers.IO) {
        // 1. 扫描物理文件 (这些对象的 isFavorite 默认为 false)
        val scannedFiles = PdfScanner.scanAllPdfFiles(context)

        // 2. 插入数据库
        // 因为用了 IGNORE，如果数据库里已有 A.pdf 且 isFavorite=true，这一步会跳过 A.pdf
        if (scannedFiles.isNotEmpty()) {
            pdfFileDao.insertAll(scannedFiles)
        }

        // 3. 关键：必须重新从数据库读取
        // 这样拿到的列表才是包含“已保留的收藏状态”的最终列表
        pdfFileDao.getAllPdfs()
    }

    // 切换收藏状态
    suspend fun toggleFavorite(path: String, isFavorite: Boolean) = withContext(Dispatchers.IO) {
        // 假设你的 PdfFileDao 中有 updateFavorite 方法，或者直接 update 整个对象
        pdfFileDao.updateFavorite(path, isFavorite)
    }

    suspend fun markAsRead(path: String) = withContext(Dispatchers.IO) {
        pdfFileDao.updateLastReadTime(path, System.currentTimeMillis())
    }

    // 可以在这里增加：删除、搜索、标记收藏等方法

    suspend fun deletePdfFile(pdf: PdfFile): Boolean = withContext(Dispatchers.IO) {
        try {
            val file = java.io.File(pdf.path)
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
                val oldFile = java.io.File(pdf.path)
                val newPath = oldFile.parent + java.io.File.separator + newName
                val newFile = java.io.File(newPath)

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

    /**
     * 获取所有 PDF 文件的流
     * 这里的 Flow 会在数据库内容变化时自动发射新列表
     */
    fun getAllPdfsFlow(): Flow<List<PdfFile>> {
        return pdfFileDao.getAllPdfsFlow()
    }
}