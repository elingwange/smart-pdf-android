package com.quantumstudio.smartpdf.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.util.CommonUtils
import com.quantumstudio.smartpdf.util.FileUtils

data class MenuOption(
    val title: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val onClick: () -> Unit
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PdfActionSheet(
    pdf: PdfFile,
    sheetState: SheetState,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = null, // 截图中似乎没有明显的 drag handle
        containerColor = Color(0xFF2C2C2C), // 深灰色背景
        shape = MaterialTheme.shapes.extraLarge // 圆角
    ) {
        Column(modifier = Modifier.padding(bottom = 32.dp)) {
            // 1. 头部文件信息
            PdfHeaderSection(pdf)

            HorizontalDivider(color = Color.DarkGray, thickness = 0.5.dp)

            // 2. 功能列表
            val options = listOf(
                MenuOption("Share", Icons.Outlined.Share) { /* 分享逻辑 */ },
                MenuOption("Rename", Icons.Outlined.Edit) { /* 重命名逻辑 */ },
                MenuOption("Details", Icons.Outlined.Info) { /* 详情逻辑 */ },
                MenuOption("Delete", Icons.Outlined.Delete) { /* 删除逻辑 */ }
            )

            options.forEach { option ->
                ListItem(
                    headlineContent = { Text(option.title, color = Color.White) },
                    leadingContent = {
                        Icon(option.icon, contentDescription = null, tint = Color.White)
                    },
                    colors = ListItemDefaults.colors(containerColor = Color.Transparent),
                    //modifier = androidx.compose.foundation.clickable { option.onClick() }
                )
            }
        }
    }
}

@Composable
fun PdfHeaderSection(pdf: PdfFile) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 重用之前的 PDF 图标逻辑
        Icon(
            painter = androidx.compose.ui.res.painterResource(id = android.R.drawable.ic_menu_report_image),
            contentDescription = null,
            tint = Color.Red,
            modifier = Modifier.size(40.dp)
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(pdf.name, color = Color.White, maxLines = 1)
            Text(
                "${CommonUtils.formatDate(pdf.uploadTime)} • ${FileUtils.formatFileSize(pdf.size)}",
                color = Color.Gray,
                fontSize = 12.sp
            )
        }

        // 收藏图标 (Bookmark)
        IconButton(onClick = { /* 收藏逻辑 */ }) {
            Icon(
                imageVector = if (pdf.isFavorite) Icons.Outlined.Info else Icons.Outlined.Share,
                contentDescription = "Favorite",
                tint = Color.White
            )
        }
    }
}