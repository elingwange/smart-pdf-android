package com.quantumstudio.smartpdf.ui.components


import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.sp

@Composable
fun ReaderMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    DropdownMenuItem(
        text = {
            Text(
                text = label,
                color = Color.White, // 确保在深色菜单背景下可见
                fontSize = 14.sp
            )
        },
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.7f) // 图标稍微淡一点，更有质感
            )
        },
        onClick = onClick
    )
}