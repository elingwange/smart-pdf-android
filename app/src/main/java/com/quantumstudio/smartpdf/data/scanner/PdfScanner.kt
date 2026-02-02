package com.quantumstudio.smartpdf.data.scanner

import android.content.Context
import android.os.Environment
import com.quantumstudio.smartpdf.data.model.PdfFile
import java.io.File

object PdfScanner {

    /**
     * 扫描手机存储中所有 PDF 文件
     */
    fun scanAllPdfFiles(context: Context): List<PdfFile> {
        val pdfList = mutableListOf<PdfFile>()

        // 根目录：外部存储
        val root = Environment.getExternalStorageDirectory()
        scanDirectory(root, pdfList)

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
