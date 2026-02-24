package com.quantumstudio.smartpdf.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.ui.theme.PdfRed

@Composable
fun SearchItem(
    pdf: PdfFile,
    query: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. 左侧图标 (类似截图中的 PDF 红标)
        Box(
            modifier = Modifier.size(40.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf, // 或者是你自定义的 PDF 图标
                contentDescription = null,
                tint = PdfRed,
                modifier = Modifier.size(32.dp)
            )
            // 如果你想更接近截图，可以叠加一个小小的 "PDF" 文字标签
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 2. 右侧信息
        Column(modifier = Modifier.weight(1f)) {
            // 高亮标题
            HighlightedText(
                text = pdf.name,
                query = query
            )

            Spacer(modifier = Modifier.height(4.dp))

            // 副标题：日期与大小 (对应截图：Feb 14, 2026 · 16.2 MB)
            Text(
                text = "${pdf.lastModified}  •  ${pdf.size}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}