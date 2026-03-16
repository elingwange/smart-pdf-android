package com.quantumstudio.smartpdf.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton // 全局单例
class PermissionManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    /**
     * 判断存储权限是否已授予
     * 适配了 Android 13 (API 33) 的权限拆分逻辑
     */
    fun isStoragePermissionGranted(): Boolean {
        val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13+ 需要检查媒体权限（根据你的 SmartPDF 需求，这里通常选 IMAGES 或 DOCUMENTS）
            Manifest.permission.READ_MEDIA_IMAGES
        } else {
            // Android 12 及以下使用旧权限
            Manifest.permission.READ_EXTERNAL_STORAGE
        }
        return ContextCompat.checkSelfPermission(context, permission) ==
                PackageManager.PERMISSION_GRANTED
    }
}