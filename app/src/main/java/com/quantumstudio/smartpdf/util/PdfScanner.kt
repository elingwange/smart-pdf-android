package com.quantumstudio.smartpdf.util

import android.content.Context
import android.provider.MediaStore
import android.util.Log
import com.quantumstudio.smartpdf.model.PdfFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PdfScanner(private val context: Context) {
    suspend fun scanPdfs(): List<PdfFile> = withContext(Dispatchers.IO) {
        val pdfList = mutableListOf<PdfFile>()

        // 使用针对外部存储的正确内容 URI
        val collection = MediaStore.Files.getContentUri("external")

        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.DATA, // 注意：API 29+ data字段被弃用，但为了获取路径仍可作为参考
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.DATE_ADDED
        )

        // 核心修改：扩大搜索范围。
        // 不仅查 MIME 类型，还通过文件名后缀查，防止模拟器识别 MIME 错误
        val selection = "${MediaStore.Files.FileColumns.MIME_TYPE} = ? OR ${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("application/pdf", "%.pdf")

        val sortOrder = "${MediaStore.Files.FileColumns.DATE_ADDED} DESC"

        try {
            context.contentResolver.query(
                collection,
                projection,
                selection,
                selectionArgs,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val pathCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATA)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)

                while (cursor.moveToNext()) {
                    val pdfFile = PdfFile(
                        id = cursor.getLong(idCol),
                        name = cursor.getString(nameCol) ?: "Unknown",
                        path = cursor.getString(pathCol) ?: "",
                        size = cursor.getLong(sizeCol),
                        dateAdded = cursor.getLong(dateCol)
                    )
                    pdfList.add(pdfFile)
                    Log.d("PdfScanner", "发现文件: ${pdfFile.name} 在 ${pdfFile.path}")
                }
            }
        } catch (e: Exception) {
            Log.e("PdfScanner", "查询出错: ${e.message}")
        }

        pdfList
    }
}