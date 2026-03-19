package com.quantumstudio.smartpdf.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
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
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // ✨ Android 11 (API 30) 及以上：检查是否拥有“所有文件访问权限”
            Environment.isExternalStorageManager()
        } else {
            // Android 10 及以下：检查传统的 READ_EXTERNAL_STORAGE
            val permission = Manifest.permission.READ_EXTERNAL_STORAGE
            ContextCompat.checkSelfPermission(context, permission) ==
                    PackageManager.PERMISSION_GRANTED
        }
    }
}