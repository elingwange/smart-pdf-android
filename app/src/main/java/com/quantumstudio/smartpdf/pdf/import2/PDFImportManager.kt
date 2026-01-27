package com.quantumstudio.smartpdf.pdf.import2

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.DocumentsContract
import androidx.activity.result.ActivityResultLauncher
import java.io.File
import java.io.IOException

class PDFImportManager(
    private val context: Context
) {

    // ---------------------------
    // Launcher 由外部传入
    // ---------------------------
    var filePickerLauncher: ActivityResultLauncher<Intent>? = null

    /**
     * 打开系统文件选择器
     */
    fun openFilePicker() {
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
        }
        filePickerLauncher?.launch(intent)
    }

    /**
     * 处理外部分享 Intent
     */
    fun handleShareIntent(intent: Intent?, onFilesPicked: (List<File>) -> Unit) {
        val uris = mutableListOf<Uri>()
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.add(it) }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                intent.getParcelableArrayListExtra<Uri>(Intent.EXTRA_STREAM)?.let { uris.addAll(it) }
            }
        }
        val files = uris.mapNotNull { handleImportedUri(it) }
        if (files.isNotEmpty()) onFilesPicked(files)
    }

    /**
     * 核心方法：校验 & 转存到私有目录
     */
    fun handleImportedUris(uris: List<Uri>, onFilesPicked: (List<File>) -> Unit) {
        val files = uris.mapNotNull { handleImportedUri(it) }
        if (files.isNotEmpty()) onFilesPicked(files)
    }

    private fun handleImportedUri(uri: Uri): File? {
        try {
            // 检查 MIME 类型
            val mimeType = context.contentResolver.getType(uri)
            if (mimeType != "application/pdf") return null

            // 检查文件头 %PDF
            context.contentResolver.openInputStream(uri)?.use { input ->
                val header = ByteArray(4)
                if (input.read(header) != 4 || !header.contentEquals("%PDF".toByteArray())) return null
            }

            // 持久化权限
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            // 获取原文件名
            val fileName = queryFileName(uri) ?: "imported.pdf"

            // 转存到私有目录
            val destFile = resolveFileNameConflict(File(context.filesDir, fileName))
            context.contentResolver.openInputStream(uri)?.use { input ->
                destFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            return destFile
        } catch (e: IOException) {
            e.printStackTrace()
            return null
        }
    }

    /**
     * 查询文件名
     */
    private fun queryFileName(uri: Uri): String? {
        var name: String? = null
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            if (cursor.moveToFirst() && idx != -1) {
                name = cursor.getString(idx)
            }
        }
        return name
    }

    /**
     * 重名处理：自动加序号
     */
    private fun resolveFileNameConflict(file: File): File {
        if (!file.exists()) return file
        val name = file.nameWithoutExtension
        val ext = file.extension
        var index = 1
        var newFile: File
        do {
            newFile = File(file.parentFile, "$name ($index).$ext")
            index++
        } while (newFile.exists())
        return newFile
    }
}
