package com.quantumstudio.smartpdf.ui.features.search.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

@Composable
fun HighlightedText(text: String, query: String) {
    val annotatedString = buildAnnotatedString {
        val startIndex = text.indexOf(query, ignoreCase = true)
        if (startIndex != -1 && query.isNotEmpty()) {
            append(text.substring(0, startIndex))
            // 橙色高亮部分
            withStyle(style = SpanStyle(background = Color(0xFFFF9800), color = Color.White)) {
                append(text.substring(startIndex, startIndex + query.length))
            }
            append(text.substring(startIndex + query.length))
        } else {
            append(text)
        }
    }
    Text(text = annotatedString, maxLines = 1, overflow = TextOverflow.Ellipsis)
}