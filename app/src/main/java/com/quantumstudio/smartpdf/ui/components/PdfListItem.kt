package com.quantumstudio.smartpdf.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.util.CommonUtils
import com.quantumstudio.smartpdf.util.FileUtils

@Composable
fun PdfListItem(
    pdf: PdfFile,
    onClick: () -> Unit,
    onMoreClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            // ✅ 改进 1：直接使用 surface。
            // 它对应你定义的 PdfSurfaceDark (0xFF1E1E1E)，比背景亮，层级感清晰。
            containerColor = MaterialTheme.colorScheme.surface
        ),
        // ✅ 改进 2：可选。如果你想让卡片更有立体感，可以加个很小的阴影
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.PictureAsPdf,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary, // PdfRed
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pdf.name,
                    style = MaterialTheme.typography.titleMedium,
                    // ✅ 改进 3：主标题强制使用 onSurface，保证最高对比度（纯白）
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // ✅ 改进 4：次要信息使用 onSurfaceVariant。
                    // 在你新定义的 Theme 中，它是较亮的灰色 (0xFFBDBDBD)，清晰且有区分度。
                    val secondaryStyle = MaterialTheme.typography.bodySmall
                    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant

                    Text(text = "${pdf.pages}P", style = secondaryStyle, color = secondaryColor)

                    Text(
                        text = " • ",
                        // ✅ 改进 5：分隔符使用 outline，它的颜色比次要文字更暗一点，视觉上不抢戏
                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
                    )

                    Text(
                        text = FileUtils.formatFileSize(pdf.size),
                        style = secondaryStyle,
                        color = secondaryColor
                    )

                    Text(text = " • ", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

                    Text(
                        text = CommonUtils.formatDate(pdf.lastModified),
                        style = secondaryStyle,
                        color = secondaryColor
                    )
                }
            }

            IconButton(onClick = onMoreClick) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "More actions",
                    // 右侧图标也建议用次要内容色
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}