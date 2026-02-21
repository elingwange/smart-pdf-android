package com.quantumstudio.smartpdf.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.quantumstudio.smartpdf.data.model.PdfFile
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CommonUtils {

    // 格式化日期：2026年1月25日
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy年M月d日", Locale.CHINESE)
        return sdf.format(Date(timestamp))
    }

    fun formatTimestamp(timestamp: Long): String {
        val sdf = java.text.SimpleDateFormat("MMM dd, yyyy h:mm:ss a", java.util.Locale.ENGLISH)
        return sdf.format(java.util.Date(timestamp))
    }

    // 你的权限请求函数，放在工具类或 MainScreen 文件末尾
    fun requestAllFilesAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        } else {
            // Android 11 以下请求常规权限，这里可以使用 rememberLauncherForActivityResult
        }
    }

    fun sharePdf(context: Context, pdf: PdfFile) {
        try {
            val file = java.io.File(pdf.path)
            // ✨ 将 File 转换为安全 URI
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                // ✨ 授予临时读取权限
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            context.startActivity(Intent.createChooser(intent, "Share PDF via"))
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot share this file", Toast.LENGTH_SHORT).show()
        }
    }
}
