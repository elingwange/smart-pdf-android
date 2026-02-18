package com.quantumstudio.smartpdf.ui.components


import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun BottomActionIcon(
    icon: ImageVector,
    contentDescription: String? = null,
    onClick: () -> Unit = {}
) {
    IconButton(onClick = onClick) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            // 使用你截图中的淡紫色调
            tint = Color(0xFF9999FF),
            modifier = Modifier.size(24.dp)
        )
    }
}