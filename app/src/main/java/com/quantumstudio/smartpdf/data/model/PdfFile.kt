package com.quantumstudio.smartpdf.data.model

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "pdf_files")
data class PdfFile(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val path: String,
    val size: Long,
    val pages: Int,
    val uploadTime: Long = System.currentTimeMillis(),
    val lastModified: Long = 0,
    val category: String? = null,
    val ownerId: String? = null,
    val version: Int = 1,
    val currentPage: Int = 0,
    val isRecent: Boolean = false,
    val isFavorite: Boolean = false
) {
    // 关键点：给这个辅助构造函数加上 @Ignore
    // 这样 Room 就会只看上面那个主构造函数，错误就会消失
    @Ignore
    constructor(
        name: String,
        path: String,
        size: Long,
        pages: Int,
        lastModified: Long = 0
    ) : this(
        id = UUID.randomUUID().toString(),
        name = name,
        path = path,
        size = size,
        pages = pages,
        uploadTime = System.currentTimeMillis(),
        lastModified = lastModified,
        category = null,
        ownerId = null,
        version = 1,
        currentPage = 0,
        isRecent = false,
        isFavorite = false
    )
}