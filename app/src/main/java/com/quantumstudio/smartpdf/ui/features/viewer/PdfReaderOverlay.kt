import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardDoubleArrowDown
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.quantumstudio.smartpdf.ui.components.BottomActionIcon
import com.quantumstudio.smartpdf.ui.components.BrightnessSliderLayout
import com.quantumstudio.smartpdf.ui.components.ReaderMenuItem
import kotlin.math.roundToInt


@Composable
fun PdfReaderOverlay(uri: Uri, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    // 状态管理
    var showMenu by remember { mutableStateOf(false) }
    // ✨ 使用 rememberSaveable 替代 remember，防止旋转后状态丢失
    var isNightMode by rememberSaveable { mutableStateOf(false) }
    var isUiVisible by rememberSaveable { mutableStateOf(true) }
    var showBrightnessSlider by rememberSaveable { mutableStateOf(false) }

    // 注意：Uri 和 PDFView 实例不能直接用 rememberSaveable 存
    // 但我们可以通过重置 lastLoadedUri 来触发 update 重新加载
    var lastLoadedUri by remember { mutableStateOf<Uri?>(null) }
    var currentPage by rememberSaveable { mutableStateOf(0) }
    var totalPages by rememberSaveable { mutableStateOf(0) }
    var pdfViewInstance by remember { mutableStateOf<PDFView?>(null) }

    val screenHeightPx =
        with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // 1. PDF 内容层
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PDFView(ctx, null).apply {
                    setOnClickListener {
                        isUiVisible = !isUiVisible
                        if (!isUiVisible) showBrightnessSlider = false
                    }
                    pdfViewInstance = this
                }
            },
            update = { pdfView ->
                // 只有文件变动或夜间模式切换时才重载（夜间模式切换通常需要 load 才能生效）
                if (lastLoadedUri != uri) {
                    pdfView.fromUri(uri)
                        .nightMode(isNightMode)
                        .onPageChange { page, count ->
                            currentPage = page
                            totalPages = count
                        }
                        .load()
                    lastLoadedUri = uri
                } else {
                    // 实时同步夜间模式
                    pdfView.setNightMode(isNightMode)
                }
            }
        )

        // 2. 右侧页码标识
        if (totalPages > 0) {
            val dragRange = screenHeightPx * 0.6f
            val yOffsetPx = (currentPage.toFloat() / (totalPages - 1).coerceAtLeast(1)) * dragRange

            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .offset { IntOffset(0, yOffsetPx.roundToInt() - (dragRange / 2).roundToInt()) }
                    .padding(end = 8.dp)
                    .size(45.dp, 30.dp)
                    .background(Color(0xFF9999FF), shape = MaterialTheme.shapes.small)
                    .draggable(
                        orientation = Orientation.Vertical,
                        state = rememberDraggableState { delta ->
                            val pageDelta = (delta / dragRange) * totalPages
                            val targetPage =
                                (currentPage + pageDelta).roundToInt().coerceIn(0, totalPages - 1)
                            pdfViewInstance?.jumpTo(targetPage)
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("${currentPage + 1}", color = Color.White, fontSize = 14.sp)
            }
        }

        // 3. ✅ 找回：头部导航栏
        AnimatedVisibility(
            visible = isUiVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.7f)
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Text(
                        text = uri.lastPathSegment ?: "PDF Reader",
                        color = Color.White,
                        fontSize = 16.sp,
                        maxLines = 1,
                        modifier = Modifier.weight(1f)
                    )
                    Box {
                        IconButton(onClick = { showMenu = true }) {
                            Icon(Icons.Default.MoreVert, "More", tint = Color.White)
                        }
                        DropdownMenu(
                            expanded = showMenu,
                            onDismissRequest = { showMenu = false },
                            modifier = Modifier.background(Color(0xFF222222))
                        ) {
                            ReaderMenuItem(Icons.Default.Info, "Info") { showMenu = false }
                            ReaderMenuItem(Icons.Default.Share, "Share") { showMenu = false }
                            ReaderMenuItem(
                                Icons.Default.FavoriteBorder,
                                "Add to favorites"
                            ) { showMenu = false }
                            ReaderMenuItem(Icons.Default.Delete, "Delete") { showMenu = false }
                        }
                    }
                }
            }
        }

        // 4. ✅ 找回：底部菜单及黑暗模式切换
        AnimatedVisibility(
            visible = isUiVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.8f)
            ) {
                Column {
                    // 亮度调节滑动条
                    if (showBrightnessSlider) {
                        BrightnessSliderLayout(activity)
                    }

                    Row(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(vertical = 12.dp)
                            .fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        BottomActionIcon(Icons.Default.ScreenRotation, onClick = {
                            activity?.let {
                                // 切换逻辑：如果是竖屏则切横屏，反之亦然
                                it.requestedOrientation =
                                    if (it.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    }
                            }
                        })

                        // ✅ 黑暗模式切换图标
                        BottomActionIcon(
                            icon = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            onClick = {
                                isNightMode = !isNightMode
                                // 注意：某些 PDFView 版本需要重新 load 才能完全应用 nightMode
                                lastLoadedUri = null
                            }
                        )

                        BottomActionIcon(
                            icon = Icons.Default.WbSunny,
                            onClick = { showBrightnessSlider = !showBrightnessSlider }
                        )
                        BottomActionIcon(Icons.Default.TouchApp)
                        BottomActionIcon(Icons.Default.KeyboardDoubleArrowDown)
                    }
                }
            }
        }
    }
}