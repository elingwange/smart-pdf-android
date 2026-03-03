package com.quantumstudio.smartpdf.ui.features.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.quantumstudio.smartpdf.R

// SearchEmptyState.kt
@Composable
fun SearchEmptyState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        // 使用 BiasAlignment 让中心点上移
        // 0f 是正中心，-1f 是最顶部，1f 是最底部。
        // -0.2f 或 -0.3f 能让内容在视觉上更舒服。
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 给整个内容组加一个上移的偏移量
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.graphicsLayer(translationY = -300f) // 直接像素级微调上移
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_no_search_found),
                contentDescription = null,
                modifier = Modifier.size(120.dp),
                tint = Color.Unspecified // ✨ 必须设为 Unspecified，才能显示 XML 定义的红色和灰色
            )

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "No Search Found",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}