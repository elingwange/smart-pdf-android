package com.quantumstudio.smartpdf.ui.components

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BrightnessSliderLayout(activity: Activity?) {
    // 获取当前窗口亮度 (0.0f 到 1.0f)
    // 如果系统亮度为默认值 (-1.0f)，我们取 0.5f 作为起始滑块位置
    var brightness by remember {
        val current = activity?.window?.attributes?.screenBrightness ?: 0.5f
        mutableStateOf(if (current < 0) 0.5f else current)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // 顶部标签栏
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 左侧小太阳
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(14.dp)
            )

            // 中间文字
            Text(
                text = "Brightness",
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), // 使用你截图中的紫色
                fontSize = 12.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )

            // 右侧大太阳
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // 亮度调节滑块
        Slider(
            value = brightness,
            onValueChange = { newValue ->
                brightness = newValue
                // 实时更新当前 Activity 的窗口亮度
                val layoutParams = activity?.window?.attributes
                layoutParams?.screenBrightness = newValue
                activity?.window?.attributes = layoutParams
            },
            // 定制滑块颜色，对齐 UI 设计
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),      // 滑块圆点颜色
                activeTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), // 已选中轨道颜色
                inactiveTrackColor = Color(0xFF333333) // 未选中轨道颜色（深灰）
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}