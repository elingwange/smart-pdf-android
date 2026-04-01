package com.quantumstudio.smartpdf.ui.features.reader.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun PdfScrollbarThumb(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    currentPage: Int,
    scrollProgress: Float,
    onScrollDelta: (Float) -> Unit
) {
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current

    // 限制指示器在屏幕中间 70% 的区域内滑动，防止撞到 TopBar 或 BottomPanel
    val trackHeightDp = configuration.screenHeightDp.dp * 0.7f
    val dragRangePx = with(density) { trackHeightDp.toPx() }

    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier,
        enter = fadeIn() + slideInHorizontally { it },
        exit = fadeOut() + slideOutHorizontally { it }
    ) {
        // 修正计算：将 0.0~1.0 映射到 -dragRangePx/2 到 dragRangePx/2
        val yOffsetPx = (scrollProgress - 0.5f) * dragRangePx

        Box(
            modifier = Modifier
                .offset { IntOffset(0, yOffsetPx.roundToInt()) }
                .size(width = 50.dp, height = 40.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary,
                    shape = RoundedCornerShape(topStart = 25.dp, bottomStart = 25.dp)
                )
                // ✨ 新增：显式消费掉按下事件，防止事件流向底层的 PDFView
                .pointerInput(Unit) {
                    detectTapGestures(onPress = { /* 拦截按下事件 */ })
                }
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta ->
                        // ✨ 调试日志：看看向上滑时 delta 是否为负数
                        // Log.d("DRAG", "delta: $delta")
                        onScrollDelta(delta / dragRangePx)
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "${currentPage + 1}",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                color = Color.White
            )
        }
    }
}