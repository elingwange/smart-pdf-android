package com.quantumstudio.smartpdf.model

// ui/models/PdfFile.kt
data class PdfFile(
    val id: Long,
    val name: String,
    val path: String,
    val size: Long,
    val dateAdded: Long
)