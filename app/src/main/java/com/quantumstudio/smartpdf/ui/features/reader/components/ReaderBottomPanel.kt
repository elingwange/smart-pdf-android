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
import androidx.compose.material.icons.filled.Brightness6
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.quantumstudio.smartpdf.ui.features.reader.PdfViewState
import com.quantumstudio.smartpdf.ui.features.reader.ReaderPanel
import com.quantumstudio.smartpdf.ui.features.reader.ReaderUiState
import com.quantumstudio.smartpdf.ui.features.reader.components.BrightnessSliderLayout
import com.quantumstudio.smartpdf.ui.features.reader.components.JumpPageLayout

@Composable
fun ReaderBottomPanel(
    modifier: Modifier = Modifier,
    uiState: ReaderUiState,    // UI 交互管家
    pdfState: PdfViewState,    // PDF 数据管家
    isFavorite: Boolean,       // 属于业务数据，保留
    onToggleFavorite: () -> Unit,
    onRotationClick: () -> Unit,
    activity: Activity?
) {
    val context = LocalContext.current

    AnimatedVisibility(
        visible = uiState.isUiVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
        modifier = modifier.fillMaxWidth()
    ) {
        Surface(
            modifier = Modifier
                .padding(horizontal = 26.dp, vertical = 26.dp)
                .navigationBarsPadding() // 确保在系统导航栏上方
                .fillMaxWidth(),
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .animateContentSize()
                    .padding(vertical = 8.dp)
            ) {
                // 内部条件渲染：直接从 uiState 和 pdfState 拿数据
                when (uiState.activePanel) {
                    ReaderPanel.Brightness -> BrightnessSliderLayout(activity, uiState)
                    ReaderPanel.Jump -> JumpPageLayout(
                        currentPage = pdfState.currentPage,
                        totalPages = pdfState.totalPages,
                        onConfirm = { target ->
                            pdfState.pdfView?.jumpTo(target)
                            uiState.closePanels()
                        }
                    )

                    ReaderPanel.None -> { /* 保持空白 */
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BottomActionIcon(Icons.Default.ScreenRotation) { onRotationClick() }

                    BottomActionIcon(if (uiState.isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode) {
                        uiState.toggleNightMode()
                        //pdfState.lastLoadedUri = null
                    }

                    BottomActionIcon(
                        icon = Icons.Default.Brightness6,
                        tint = if (uiState.activePanel == ReaderPanel.Brightness)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = {
                            val windowBrightness =
                                activity?.window?.attributes?.screenBrightness ?: -1f
                            val finalBrightness = if (windowBrightness < 0f) {
                                // ✨ 读取系统全局亮度设置 (范围 0-255) 并转换为 0-1f
                                try {
                                    android.provider.Settings.System.getInt(
                                        context.contentResolver,
                                        android.provider.Settings.System.SCREEN_BRIGHTNESS
                                    ) / 255f
                                } catch (e: Exception) {
                                    0.5f // 保底方案
                                }
                            } else {
                                windowBrightness
                            }
                            uiState.toggleBrightness(finalBrightness)
                        }
                    )

                    BottomActionIcon(
                        icon = Icons.Default.FindInPage,
                        tint = if (uiState.activePanel == ReaderPanel.Jump)
                            MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        onClick = { uiState.toggleJump() }
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