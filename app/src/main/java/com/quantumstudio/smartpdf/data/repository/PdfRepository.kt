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
        // 1. 扫描物理文件
        val scannedFiles = PdfScanner.scanAllPdfFiles(context)

        // 2. 存入数据库（由于 path 是主键，REPLACE 会自动处理重复问题）
        if (scannedFiles.isNotEmpty()) {
            pdfFileDao.insertAll(scannedFiles)
        }

        // 3. 统一从数据库返回（这样能保留用户的收藏、阅读进度等信息）
        pdfFileDao.getAllPdfs()
    }

    // 可以在这里增加：删除、搜索、标记收藏等方法
}