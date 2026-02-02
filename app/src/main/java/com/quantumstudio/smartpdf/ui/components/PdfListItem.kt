package com.quantumstudio.smartpdf.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.util.CommonUtils
import com.quantumstudio.smartpdf.util.FileUtils

@Composable
fun PdfListItem(pdf: PdfFile, onMenuClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. PDF 图标 (根据截图是红底白色 PDF 字样)
        // 这里假设你有一个名为 ic_pdf 的资源，如果没有可以先用 Box 代替
        Box(
            modifier = Modifier
                .size(45.dp)
                .background(Color.Transparent), // 实际图片自带背景
            contentAlignment = Alignment.Center
        ) {
            // 替换为你项目中真实的 PDF icon 资源
            Icon(
                painter = painterResource(id = android.R.drawable.ic_menu_report_image),
                contentDescription = "PDF Icon",
                tint = Color.Red,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        // 2. 文件信息 (标题 + 详情行)
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = pdf.name,
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White // 截图是深色模式
                ),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(
                    text = CommonUtils.formatDate(pdf.uploadTime),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = " • ",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
                Text(
                    text = FileUtils.formatFileSize(pdf.size),
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
        }

        // 3. 更多按钮
        IconButton(onClick = onMenuClick) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "More",
                tint = Color.LightGray
            )
        }
    }
}
