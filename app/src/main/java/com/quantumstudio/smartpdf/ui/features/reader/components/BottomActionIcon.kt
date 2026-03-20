package com.quantumstudio.smartpdf.ui.features.reader.components


import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun BottomActionIcon(
    icon: ImageVector,
    contentDescription: String? = null,
    // ✨ 增加 tint 参数，允许外部传入颜色
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    onClick: () -> Unit = {}
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            // ✨ 使用传入的 tint，而不是硬编码
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}