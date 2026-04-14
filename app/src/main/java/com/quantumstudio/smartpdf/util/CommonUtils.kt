package com.quantumstudio.smartpdf.util

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.core.content.FileProvider
import com.quantumstudio.smartpdf.R
import com.quantumstudio.smartpdf.data.model.PdfFile
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat

object CommonUtils {
    /**
     * 切换屏幕方向：横屏 <-> 竖屏
     */
    fun toggleScreenOrientation(activity: Activity?) {
        activity?.let {
            it.requestedOrientation =
                if (it.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
        }
    }

    // 格式化日期：根据设备当前的 Locale（如 en_US 或 zh_CN）自动排版
    // 美国显示: Apr 14, 2026
    // 中国显示: 2026年4月14日
    fun formatDate(timestamp: Long): String {
        val label = DateFormat.getDateInstance(DateFormat.MEDIUM).format(timestamp)
        return label
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

    fun openAppInfoSettings(context: Context) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            // 直接定位到 Smart PDF 的系统详情页
            data = android.net.Uri.fromParts("package", context.packageName, null)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    fun triggerDefaultPdfPicker(context: Context) {
        try {
            // 1. 创建一个临时的空 PDF 文件（仅用于触发系统识别）
            val tempFile = java.io.File(context.cacheDir, "default_check.pdf")
            if (!tempFile.exists()) tempFile.createNewFile()

            // 2. 获取文件的 Uri (需要你配置过 FileProvider)
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                tempFile
            )

            // 3. 构建查看 PDF 的 Intent
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            // 4. 显式弹出选择器，标题可以提示用户“请选择本应用并点‘始终’”
            context.startActivity(
                android.content.Intent.createChooser(
                    intent,
                    "Set as Default PDF Viewer"
                )
            )
        } catch (e: Exception) {
            // 如果失败，降级方案：跳转到应用详情页
            val detailIntent =
                android.content.Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    .apply {
                        data = android.net.Uri.fromParts("package", context.packageName, null)
                    }
            context.startActivity(detailIntent)
        }
    }

    fun getAppVersionName(context: Context): String {
        return try {
            val packageManager = context.packageManager // ✨ 必须通过 context 获取实例
            val packageName = context.packageName

            val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                // Android 13+ 使用新的 flag 方式
                packageManager.getPackageInfo(
                    packageName,
                    PackageManager.PackageInfoFlags.of(0) // 确保 PackageManager 首字母大写
                )
            } else {
                // 旧版本兼容
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
            }
            packageInfo.versionName ?: "Unknown"
        } catch (e: Exception) {
            e.printStackTrace()
            "Unknown"
        }
    }

    fun openPlayStore(context: Context) {
        val packageName = context.packageName
        try {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("market://details?id=$packageName")
                )
            )
        } catch (e: Exception) {
            context.startActivity(
                Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
                )
            )
        }
    }

    fun sendFeedbackEmail(context: Context) {
        val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
            val appVersion = getAppVersionName(context) // 复用已有的安全函数
            // mailto: 协议确保只匹配邮件客户端
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf("support@yourdomain.com")) // 收件人
            putExtra(Intent.EXTRA_SUBJECT, "Feedback for Smart PDF v${appVersion}")       // 主件
            // 在 sendFeedbackEmail 内部
            val debugInfo =
                "\n\n--- Debug Info ---\nApp Version: $appVersion\nDevice: ${Build.MODEL}"
            putExtra(
                Intent.EXTRA_TEXT,
                "Hi Team,\n\nI have some feedback regarding...\n\n" + debugInfo
            ) // 正文
        }

        try {
            context.startActivity(Intent.createChooser(emailIntent, "Send feedback via..."))
        } catch (e: Exception) {
            // 容错处理：如果用户手机上没有安装任何邮件应用
            Toast.makeText(context, "No email app installed", Toast.LENGTH_SHORT).show()
        }
    }

    fun openPrivacyPolicy(context: Context) {
        val intent =
            Intent(Intent.ACTION_VIEW, Uri.parse("https://sites.google.com/view/smart-pdf-mate/"))
        context.startActivity(intent)
    }

    fun printPdf(context: Context, file: File?, name: String?) {
        val printManager =
            context.getSystemService(Context.PRINT_SERVICE) as? android.print.PrintManager
        val jobName = "${context.getString(R.string.app_name)} - $name"

        // 使用系统内置的 PrintDocumentAdapter 发送文件流
        printManager?.print(
            jobName,
            object : android.print.PrintDocumentAdapter() {
                override fun onWrite(
                    pages: Array<out android.print.PageRange>?,
                    destination: android.os.ParcelFileDescriptor?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: WriteResultCallback?
                ) {
                    try {
                        val input = java.io.FileInputStream(file)
                        val output = java.io.FileOutputStream(destination?.fileDescriptor)
                        input.copyTo(output)
                        callback?.onWriteFinished(arrayOf(android.print.PageRange.ALL_PAGES))
                    } catch (e: Exception) {
                        callback?.onWriteFailed(e.message)
                    }
                }

                override fun onLayout(
                    oldAttributes: android.print.PrintAttributes?,
                    newAttributes: android.print.PrintAttributes?,
                    cancellationSignal: android.os.CancellationSignal?,
                    callback: LayoutResultCallback?,
                    extras: android.os.Bundle?
                ) {
                    if (cancellationSignal?.isCanceled == true) {
                        callback?.onLayoutCancelled()
                        return
                    }
                    // 告诉系统 PDF 已准备好
                    if (name != null) {
                        val pdi = android.print.PrintDocumentInfo.Builder(name)
                            .setContentType(android.print.PrintDocumentInfo.CONTENT_TYPE_DOCUMENT)
                            .build()
                        callback?.onLayoutFinished(pdi, true)
                    }
                }
            },
            null
        )
    }

    fun uriToFile(context: Context, uri: Uri): File? {
        // 1. 如果是 file:// 协议，直接获取路径
        if (uri.scheme == "file") {
            return uri.path?.let { File(it) }
        }

        // 2. 如果是 content:// 协议（如从文件管理器选择的）
        return try {
            val contentResolver = context.contentResolver
            // 获取文件名（可选，用于保持临时文件后缀一致）
            val fileName = "temp_pdf_${System.currentTimeMillis()}.pdf"

            // 创建缓存文件
            val tempFile = File(context.cacheDir, fileName)

            contentResolver.openInputStream(uri)?.use { inputStream ->
                FileOutputStream(tempFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
