package com.quantumstudio.smartpdf.pdf

import PdfMetadataExtractor
import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class PdfManager(private val context: Context, private val dao: PdfFileDao) {

    private val appDir: File = context.filesDir // 私有目录

    /**
     * 导入 PDF
     */
    suspend fun importPdf(sourceFile: File, category: String? = null) {
        withContext(Dispatchers.IO) {
            // 1. 复制文件到私有目录
            val destFile = File(appDir, sourceFile.name)
            sourceFile.copyTo(destFile, overwrite = true)

            // 2. 提取元数据
            val pdfMeta = PdfMetadataExtractor.extractMetadata(destFile).copy(category = category)

            // 3. 保存到数据库
            dao.insert(pdfMeta)
        }
    }

    /**
     * 删除 PDF
     */
    suspend fun deletePdf(pdf: PdfFile) {
        withContext(Dispatchers.IO) {
            val file = File(pdf.path)
            if (file.exists()) file.delete()
            dao.delete(pdf)
        }
    }

    /**
     * 获取所有 PDF
     */
    suspend fun getAllPdfs(): List<PdfFile> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    /**
     * 搜索 PDF
     */
    suspend fun searchPdfs(keyword: String): List<PdfFile> = withContext(Dispatchers.IO) {
        dao.searchByName(keyword)
    }
}
