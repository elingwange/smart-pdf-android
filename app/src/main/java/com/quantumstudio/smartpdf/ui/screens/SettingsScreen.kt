package com.quantumstudio.smartpdf.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.ThumbUpOffAlt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212)) // 跟主背景保持一致
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // 支持长页面滚动
    ) {
        // 第一组：通用设置
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)), // 稍浅的卡片背景
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                SettingRow(Icons.Outlined.Folder, "File Manager") { /* 处理点击 */ }
                SettingRow(Icons.Outlined.SettingsSuggest, "Set as Default") { /* 处理点击 */ }
                SettingRow(Icons.Outlined.Language, "Language") { /* 处理点击 */ }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 第二组：关于与反馈
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                SettingRow(Icons.Outlined.Share, "Share") { /* 处理点击 */ }
                SettingRow(Icons.Outlined.ThumbUpOffAlt, "Rate us") { /* 处理点击 */ }
                SettingRow(Icons.Outlined.Feedback, "Feedback") { /* 处理点击 */ }
                SettingRow(Icons.Outlined.PrivacyTip, "Privacy policy") { /* 处理点击 */ }
                // 版本号行，右侧带有副标题
                SettingRow(
                    icon = Icons.Outlined.Info,
                    title = "Version",
                    subtitle = "6.3.2"
                ) { /* 处理点击 */ }
            }
        }
    }
}

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    // 建议使用 Surface 包裹，因为它能提供更标准的点击波纹效果和层级处理
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            // 关键：暂时不使用 Alignment.CenterVertically 这种可能触发重测绘的参数
            // 改用基础的对齐方式
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            // 关键修改：不要给 Text 设置 weight，除非 subtitle 真的需要靠右对齐
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White,
                modifier = Modifier.align(Alignment.CenterVertically)
            )

            if (subtitle != null) {
                // 如果需要靠右对齐，用这个 Spacer 撑开，这是最稳妥的 weight 使用位置
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray,
                    modifier = Modifier.align(Alignment.CenterVertically)
                )
            }
        }
    }
}