package com.quantumstudio.smartpdf.ui.features.reader.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt


@Composable
fun PdfScrollbarThumb(
    modifier: Modifier = Modifier, // 允许外部传入 align
    isVisible: Boolean,
    currentPage: Int,
    scrollProgress: Float,
    onScrollDelta: (Float) -> Unit
) {
    val screenHeightPx =
        with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
    val dragRange = screenHeightPx * 0.7f

    AnimatedVisibility(
        visible = isVisible,
        modifier = modifier, // 应用外部传入的对齐逻辑
        enter = fadeIn() + slideInHorizontally { it },
        exit = fadeOut() + slideOutHorizontally { it }
    ) {
        val yOffsetPx = scrollProgress * dragRange
        Box(
            modifier = Modifier
                .offset { IntOffset(0, (yOffsetPx - dragRange / 2).roundToInt()) }
                .size(42.dp, 30.dp)
                .background(
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                    RoundedCornerShape(topStart = 20.dp, bottomStart = 20.dp)
                )
                .draggable(
                    orientation = Orientation.Vertical,
                    state = rememberDraggableState { delta -> onScrollDelta(delta / dragRange) }
                ),
            contentAlignment = Alignment.Center
        ) {
            Text("${currentPage + 1}", color = Color.White, fontWeight = FontWeight.Bold)
        }
    }
}