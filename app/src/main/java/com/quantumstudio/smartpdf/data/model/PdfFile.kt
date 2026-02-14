package com.quantumstudio.smartpdf.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey

@Entity(tableName = "pdf_files")
data class PdfFile(
    @PrimaryKey
    val path: String,        // 使用绝对路径作为主键，不再使用 UUID
    val name: String,
    val size: Long,
    val pages: Int,
    val lastModified: Long,
    val uploadTime: Long = System.currentTimeMillis(),
    val category: String? = null,
    val ownerId: String? = null,
    val version: Int = 1,
    val currentPage: Int = 0,
    val isRecent: Boolean = false,
    val isFavorite: Boolean = false
) {
    // 删掉之前的辅助构造函数，或者改成这样：
    @Ignore
    constructor(
        name: String,
        path: String,
        size: Long,
        pages: Int,
        lastModified: Long
    ) : this(
        path = path,
        name = name,
        size = size,
        pages = pages,
        lastModified = lastModified,
        uploadTime = System.currentTimeMillis(),
        category = null,
        ownerId = null,
        version = 1,
        currentPage = 0,
        isRecent = false,
        isFavorite = false
    )
}