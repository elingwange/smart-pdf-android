package com.quantumstudio.smartpdf.util

import android.content.Context
import android.net.Uri
import android.util.Log
import java.io.File

object UriResolver {
    fun resolveToTempFile(context: Context, source: String): File? {
        val uri = Uri.parse(source)
        val targetFile = File(context.cacheDir, "reader_temp_${System.currentTimeMillis()}.pdf")

        return try {
            // 尝试直接读（如果是 App 自己的文件或已授权文件）
            val originalFile = File(uri.path ?: "")
            if (originalFile.exists() && originalFile.canRead()) {
                return originalFile
            }

            // 【核心急救】：通过 ContentResolver 强行拷贝流
            context.contentResolver.openInputStream(uri)?.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }

            if (targetFile.exists() && targetFile.length() > 0) targetFile else null
        } catch (e: Exception) {
            Log.e("PDF_EMERGENCY", "Resolve failed: ${e.message}")
            null
        }
    }
}