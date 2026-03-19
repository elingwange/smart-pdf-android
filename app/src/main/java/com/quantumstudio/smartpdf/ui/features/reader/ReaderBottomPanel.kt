package com.quantumstudio.smartpdf.ui.features.reader

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quantumstudio.smartpdf.ui.components.BottomActionIcon
import com.quantumstudio.smartpdf.ui.components.BrightnessSliderLayout
import com.quantumstudio.smartpdf.ui.components.JumpPageLayout


@Composable
fun ReaderBottomPanel(
    modifier: Modifier = Modifier,
    isVisible: Boolean,
    isFavorite: Boolean,
    isNightMode: Boolean,
    currentPage: Int,
    totalPages: Int,
    showBrightnessSlider: Boolean,
    showJumpLayout: Boolean,
    onToggleBrightness: () -> Unit,
    onToggleJump: () -> Unit,
    onToggleNightMode: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRotationClick: () -> Unit,
    onJumpToPage: (Int) -> Unit,
    activity: Activity? // 用于亮度控制
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 8.dp
        ) {
            Column(modifier = Modifier.animateContentSize()) {
                // 内部条件渲染：亮度条 vs 跳转框
                when {
                    showBrightnessSlider -> BrightnessSliderLayout(activity)
                    showJumpLayout -> JumpPageLayout(currentPage, totalPages, onJumpToPage)
                }

                Row(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    BottomActionIcon(Icons.Default.ScreenRotation) { onRotationClick() }

                    BottomActionIcon(if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode) {
                        onToggleNightMode()
                    }

                    BottomActionIcon(
                        icon = Icons.Default.WbSunny,
                        tint = if (showBrightnessSlider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onToggleBrightness
                    )

                    BottomActionIcon(
                        icon = Icons.Default.FindInPage,
                        tint = if (showJumpLayout) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onToggleJump
                    )

                    BottomActionIcon(
                        icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = onToggleFavorite
                    )
                }
            }
        }
    }
}