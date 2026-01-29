package com.quantumstudio.smartpdf.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object FileUtils {

    // 格式化日期：2026年1月25日
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy年M月d日", Locale.CHINESE)
        return sdf.format(Date(timestamp))
    }

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
}