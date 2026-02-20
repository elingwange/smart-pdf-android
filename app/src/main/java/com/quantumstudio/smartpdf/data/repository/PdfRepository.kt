package com.quantumstudio.smartpdf.data.repository

import android.content.Context
import com.quantumstudio.smartpdf.data.local.PdfFileDao
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.data.scanner.PdfScanner
import kotlinx.coroutines.Dispatchers
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

    // 可以在这里增加：删除、搜索、标记收藏等方法
}