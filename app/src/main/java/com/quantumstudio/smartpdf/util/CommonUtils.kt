package com.quantumstudio.smartpdf.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object CommonUtils {

    // 格式化日期：2026年1月25日
    fun formatDate(timestamp: Long): String {
        val sdf = SimpleDateFormat("yyyy年M月d日", Locale.CHINESE)
        return sdf.format(Date(timestamp))
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
}
