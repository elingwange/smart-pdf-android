package com.quantumstudio.smartpdf.ui.features.main.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quantumstudio.smartpdf.R
import com.quantumstudio.smartpdf.data.model.PdfFile

@Composable
fun PdfListItem(
    pdf: PdfFile,
    onClick: () -> Unit,
    // ✨ 修改：将 onMoreClick 改为动作回调，方便统一处理逻辑
    onMenuAction: (MenuAction) -> Unit
) {
    // 💡 状态管理：控制菜单显示/隐藏
    var showMenu by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            // ✨ 建议：深色模式下稍微拉开一点色差
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow
        ),
        // ✨ 核心修补：增加描边。浅色模式透明，深色模式显示淡淡的灰色/主色描边
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_pdf),
                contentDescription = "PDF Icon",
                tint = Color.Unspecified,
                modifier = Modifier.size(40.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pdf.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // ... 次要信息 Row 部分保持不变 ...
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val secondaryStyle = MaterialTheme.typography.bodySmall
                    val secondaryColor = MaterialTheme.colorScheme.onSurfaceVariant
                    Text(text = "${pdf.pages}P", style = secondaryStyle, color = secondaryColor)
                    Text(text = " • ", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Text(
                        text = pdf.sizeLabel,
                        style = secondaryStyle,
                        color = secondaryColor
                    )
                    Text(text = " • ", color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
                    Text(
                        text = pdf.lastModifiedLabel,
                        style = secondaryStyle,
                        color = secondaryColor
                    )
                }
            }

            // ✨ 改进：集成三圆点菜单逻辑
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More actions",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // 弹出式菜单
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    // 简化写法：直接使用属性设置背景色
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    // 复用你之前定义的 ReaderMenuItem 结构
                    ListItemMenu(
                        isFavorite = pdf.isFavorite,
                        onAction = { action ->
                            showMenu = false
                            onMenuAction(action)
                        }
                    )
                }
            }
        }
    }
}

sealed class MenuAction {
    object Info : MenuAction()
    object Share : MenuAction()
    object Rename : MenuAction()
    object Favorite : MenuAction()
    object Delete : MenuAction()
}

@Composable
fun ListItemMenu(
    isFavorite: Boolean,
    onAction: (MenuAction) -> Unit
) {
    // Info
    ReaderMenuItem(
        icon = Icons.Default.Info,
        label = "Info",
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    ) { onAction(MenuAction.Info) }

    // Share
    ReaderMenuItem(
        icon = Icons.Default.Share,
        label = "Share",
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    ) { onAction(MenuAction.Share) }

    // Rename (假设你导入了 Edit 图标)
    ReaderMenuItem(
        icon = Icons.Default.Edit,
        label = "Rename",
        tint = MaterialTheme.colorScheme.onSurfaceVariant
    ) { onAction(MenuAction.Rename) }

    // Favorite
    ReaderMenuItem(
        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
        label = if (isFavorite) "Remove from favorites" else "Add to favorites",
        tint = if (isFavorite) Color(0xFF9999FF) else MaterialTheme.colorScheme.onSurfaceVariant
    ) { onAction(MenuAction.Favorite) }

    // Delete
    ReaderMenuItem(
        icon = Icons.Default.Delete,
        label = "Delete",
        tint = MaterialTheme.colorScheme.error // ✨ 删除使用 error 颜色（通常是红色）
    ) { onAction(MenuAction.Delete) }
}