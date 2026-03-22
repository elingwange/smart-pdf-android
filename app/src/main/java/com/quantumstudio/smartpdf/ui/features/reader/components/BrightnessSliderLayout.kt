package com.quantumstudio.smartpdf.ui.features.reader.components

import android.app.Activity
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumstudio.smartpdf.ui.features.reader.ReaderUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BrightnessSliderLayout(activity: Activity?, uiState: ReaderUiState) {

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

//        Slider(
//            value = if (uiState.currentBrightness < 0f) 0.5f else uiState.currentBrightness,
//            onValueChange = { newValue ->
//                uiState.updateBrightness(newValue)
//                activity?.let {
//                    val lp = it.window.attributes
//                    lp.screenBrightness = newValue
//                    it.window.attributes = lp
//                }
//            },
//            modifier = Modifier.fillMaxWidth()
//        )

        ModernBrightnessBar(
            currentValue = if (uiState.currentBrightness < 0f) 0.5f else uiState.currentBrightness,
            onValueChange = { newValue ->
                uiState.updateBrightness(newValue)
                activity?.let {
                    val lp = it.window.attributes
                    lp.screenBrightness = newValue
                    it.window.attributes = lp
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp), // 完美的零边距对齐
            barHeight = 10.dp, // 轨道高度
            thumbColor = Color.White // 你也可以用 MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun ModernBrightnessBar(
    currentValue: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier,
    barHeight: Dp = 6.dp, // 减细轨道，更精致
    activeColor: Color = MaterialTheme.colorScheme.primary,
    inactiveColor: Color = MaterialTheme.colorScheme.outlineVariant,
    thumbColor: Color = Color.White
) {
    val progress = currentValue.coerceIn(0f, 1f)

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            // ✨ 容器高度设为 32.dp，提供足够的纵向点击热区和滑块显示空间
            .height(32.dp)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    onValueChange((offset.x / size.width).coerceIn(0f, 1f))
                }
            }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    onValueChange((change.position.x / size.width).coerceIn(0f, 1f))
                }
            }
    ) {
        val width = constraints.maxWidth.toFloat()
        val height = constraints.maxHeight.toFloat() // 这里的 height 是 32.dp 的像素值
        val thumbCenterX = width * progress

        Canvas(modifier = Modifier.fillMaxSize()) {
            // ✨ 1. 计算轨道的垂直居中位置
            val barHeightPx = barHeight.toPx()
            val barTop = (height - barHeightPx) / 2
            val cornerRadius = CornerRadius(barHeightPx / 2, barHeightPx / 2)

            // 2. 绘制背景轨道（居中）
            drawRoundRect(
                color = inactiveColor,
                topLeft = Offset(0f, barTop),
                size = Size(width, barHeightPx),
                cornerRadius = cornerRadius
            )

            // 3. 绘制进度（居中）
            drawRoundRect(
                color = activeColor,
                topLeft = Offset(0f, barTop),
                size = Size(thumbCenterX, barHeightPx),
                cornerRadius = cornerRadius
            )

            // ✨ 4. 绘制更有层次感的滑块 (Thumb)
            val thumbWidth = 4.dp.toPx()
            // 滑块比轨道高出很多，形成明显的纵向对比
            val thumbHeight = 18.dp.toPx()

            drawRoundRect(
                color = thumbColor,
                topLeft = Offset(
                    x = (thumbCenterX - thumbWidth / 2).coerceIn(0f, width - thumbWidth),
                    y = (height - thumbHeight) / 2 // 滑块也在容器内居中
                ),
                size = Size(thumbWidth, thumbHeight),
                cornerRadius = CornerRadius(thumbWidth / 2, thumbWidth / 2)
            )
        }
    }
}