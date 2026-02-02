package com.quantumstudio.smartpdf.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.ui.features.viewer.PDFViewerActivity
import java.io.File

object FileUtils {

    // 格式化文件大小：B, KB, MB
    fun formatFileSize(size: Long): String {
        if (size <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB")
        val digitGroups = (Math.log10(size.toDouble()) / Math.log10(1024.0)).toInt()
        return String.format(
            "%.1f %s",
            size / Math.pow(1024.0, digitGroups.toDouble()),
            units[digitGroups]
        )
    }

    fun openPdf(context: Context, pdf: PdfFile) {
        // 直接在这里处理跳转逻辑
        val intent = Intent(context, PDFViewerActivity::class.java).apply {
            putExtra("pdf_uri", Uri.fromFile(File(pdf.path)))
        }
        context.startActivity(intent)
    }
}