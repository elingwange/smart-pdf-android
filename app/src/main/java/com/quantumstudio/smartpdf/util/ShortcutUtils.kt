package com.quantumstudio.smartpdf.util

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.ShortcutInfo
import android.content.pm.ShortcutManager
import android.graphics.drawable.Icon // ✨ 修正 1：确保导入的是 android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.quantumstudio.smartpdf.MainActivity
import com.quantumstudio.smartpdf.R
import com.quantumstudio.smartpdf.data.model.PdfFile

object ShortcutUtils {

    // ✨ 修正 2：移除 @Composable 和 @OptIn(ExperimentalMaterial3Api::class)
    // 这是一个后台逻辑函数，不是 UI 组件
    @RequiresApi(Build.VERSION_CODES.O) // ✨ 修正 3：PinShortcut 需要 API 26 (Android 8.0)
    fun addPdfToHomeScreen(context: Context, pdf: PdfFile) {
        val shortcutManager = context.getSystemService(ShortcutManager::class.java) ?: return

        if (shortcutManager.isRequestPinShortcutSupported) {
            // 1. 构建 Intent
            val intent = Intent(context, MainActivity::class.java).apply {
                action = Intent.ACTION_VIEW
                // ✨ 修正 4：确保路径转为 Uri，且处理可能的特殊字符
                data =
                    Uri.parse(if (pdf.path.startsWith("content://")) pdf.path else "file://${pdf.path}")
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }

            // 2. 创建快捷方式信息
            val pinShortcutInfo = ShortcutInfo.Builder(context, pdf.path)
                .setShortLabel(pdf.name.take(10)) // 桌面图标文字不宜过长
                .setLongLabel(pdf.name)
                .setIcon(
                    Icon.createWithResource(
                        context,
                        R.drawable.ic_pdf // 确保这个资源存在且是 Vector 或 PNG
                    )
                )
                .setIntent(intent)
                .build()

            // 3. 请求添加
            val pinnedShortcutCallbackIntent =
                shortcutManager.createShortcutResultIntent(pinShortcutInfo)

            // ✨ 修正 5：PendingIntent 标志位适配
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT
            }

            val successCallback = PendingIntent.getBroadcast(
                context,
                pdf.path.hashCode(), // 使用唯一请求码防止冲突
                pinnedShortcutCallbackIntent,
                flags
            )

            shortcutManager.requestPinShortcut(pinShortcutInfo, successCallback.intentSender)
        } else {
            Toast.makeText(context, "Launcher does not support shortcuts", Toast.LENGTH_SHORT)
                .show()
        }
    }
}