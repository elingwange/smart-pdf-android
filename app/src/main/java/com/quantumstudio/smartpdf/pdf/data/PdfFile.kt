package com.quantumstudio.smartpdf.pdf.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "pdf_files")
data class PdfFile(
    @PrimaryKey val id: String = UUID.randomUUID().toString(), // 唯一 ID
    val name: String,        // 文件名
    val path: String,        // 私有目录路径
    val size: Long,          // 文件大小（字节）
    val pages: Int,          // 页数
    val uploadTime: Long = System.currentTimeMillis(),
    val category: String? = null,    // 分类
    val ownerId: String? = null,     // 用户 ID
    val version: Int = 1,            // 版本号
    val isFavorite: Boolean = false  // 收藏标记
)
