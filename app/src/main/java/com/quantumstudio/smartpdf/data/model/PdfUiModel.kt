package com.quantumstudio.smartpdf.data.model

// 定义一个专门给 UI 用的类（普通 POJO，不带数据库逻辑）
data class PdfUiModel(
    val path: String,
    val name: String,
    val sizeLabel: String,         // 已经算好了的字符串
    val lastModifiedLabel: String  // 已经算好了的字符串
)