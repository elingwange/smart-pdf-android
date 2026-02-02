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
    /**
     * 获取所有 PDF 文件
     * 逻辑：从扫描器获取最新文件 -> 存入数据库 -> 从数据库读取展示
     */
    suspend fun getAllPdfs(context: Context): List<PdfFile> = withContext(Dispatchers.IO) {
        try {
            // 1. 调用你之前的扫描工具类
            val scannedFiles = PdfScanner.scanAllPdfFiles(context)

            // 2. 将扫描到的文件同步到数据库 (Upsert 逻辑)
            if (scannedFiles.isNotEmpty()) {
                pdfFileDao.insertAll(scannedFiles)
            }

            // 3. 始终以数据库为准返回数据，确保收藏状态等本地字段不丢失
            pdfFileDao.getAllPdfs()
        } catch (e: Exception) {
            e.printStackTrace()
            // 如果扫描失败，至少尝试返回数据库现有的数据
            pdfFileDao.getAllPdfs()
        }
    }

    // 可以在这里增加：删除、搜索、标记收藏等方法
}