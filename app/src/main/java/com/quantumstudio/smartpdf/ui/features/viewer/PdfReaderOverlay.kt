import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import kotlinx.coroutines.delay
import kotlin.math.roundToInt


@Composable
fun PdfReaderOverlay(uri: Uri, onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity

    // 1. 状态管理
    var showMenu by remember { mutableStateOf(false) }
    var isNightMode by rememberSaveable { mutableStateOf(false) }
    var isUiVisible by rememberSaveable { mutableStateOf(true) }
    var showBrightnessSlider by rememberSaveable { mutableStateOf(false) }

    var lastLoadedUri by remember { mutableStateOf<Uri?>(null) }
    var currentPage by rememberSaveable { mutableStateOf(0) }
    var totalPages by rememberSaveable { mutableStateOf(0) }
    var pdfViewInstance by remember { mutableStateOf<PDFView?>(null) }

    val screenHeightPx =
        with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }

    // ✨ 控制显隐逻辑：页码指示器
    var isPageIndicatorVisible by remember { mutableStateOf(false) }
    var scrollSignal by remember { mutableStateOf(0L) }

    // 只要 scrollSignal 变动（滑动或拖动），就显示 2 秒后消失
    LaunchedEffect(scrollSignal) {
        if (totalPages > 0) {
            isPageIndicatorVisible = true
            delay(1300)
            isPageIndicatorVisible = false
        }
    }

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
                if (lastLoadedUri != uri) {
                    pdfView.fromUri(uri)
                        .nightMode(isNightMode)
                        .defaultPage(currentPage)
                        .onPageChange { page, count ->
                            currentPage = page
                            totalPages = count
                        }
                        // ✨ 核心：捕获任何页内或跨页的滑动位移
                        .onPageScroll { _, _ ->
                            scrollSignal = System.currentTimeMillis()
                        }
                        .load()
                    lastLoadedUri = uri
                } else {
                    pdfView.setNightMode(isNightMode)
                }
            }
        )

        // 2. 右侧页码标识 (自动显隐动画)
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            AnimatedVisibility(
                visible = isPageIndicatorVisible,
                // 从右侧切入切出
                enter = fadeIn() + slideInHorizontally { it },
                exit = fadeOut() + slideOutHorizontally { it }
            ) {
                val dragRange = screenHeightPx * 0.6f
                val yOffsetPx =
                    (currentPage.toFloat() / (totalPages - 1).coerceAtLeast(1)) * dragRange

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                0,
                                yOffsetPx.roundToInt() - (dragRange / 2).roundToInt()
                            )
                        }
                        .padding(end = 1.dp)
                        .size(45.dp, 30.dp)
                        .background(
                            color = Color(0xFF9999FF),
                            // ✨ 关键：只设置左侧的圆角 (TopStart 和 BottomStart)
                            shape = RoundedCornerShape(
                                topStart = 18.dp,
                                bottomStart = 18.dp,
                                topEnd = 0.dp,
                                bottomEnd = 0.dp
                            )
                        )
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                // ✨ 拖动时同步更新信号，防止拖动过程中标识突然消失
                                scrollSignal = System.currentTimeMillis()
                                val pageDelta = (delta / dragRange) * totalPages
                                val targetPage = (currentPage + pageDelta).roundToInt()
                                    .coerceIn(0, totalPages - 1)
                                if (targetPage != currentPage) {
                                    pdfViewInstance?.jumpTo(targetPage)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    // 文字稍微向左偏移一点点，视觉上更居中（因为右侧贴边了）
                    Text(
                        text = "${currentPage + 1}",
                        color = Color.White,
                        fontSize = 15.sp,
                        modifier = Modifier.padding(end = 2.dp)
                    )
                }
            }
        }

        // 3. 头部导航栏 (逻辑保持不变)
        AnimatedVisibility(
            visible = isUiVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color.Black.copy(alpha = 0.9f)
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
                    // ... DropdownMenu 部分保持不变 ...
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

        // 4. 底部菜单 (增加旋转强制刷新)
        AnimatedVisibility(
            visible = isUiVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            Surface(modifier = Modifier.fillMaxWidth(), color = Color.Black.copy(alpha = 0.9f)) {
                Column {
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
                                lastLoadedUri = null // ✨ 旋转时清空缓存，强制 AndroidView 重新 load()
                                it.requestedOrientation =
                                    if (it.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) {
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                                    } else {
                                        ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                                    }
                            }
                        })
                        BottomActionIcon(
                            icon = if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode,
                            onClick = { isNightMode = !isNightMode; lastLoadedUri = null }
                        )
                        BottomActionIcon(
                            Icons.Default.WbSunny,
                            onClick = { showBrightnessSlider = !showBrightnessSlider })
                        BottomActionIcon(Icons.Default.TouchApp)
                        BottomActionIcon(Icons.Default.KeyboardDoubleArrowDown)
                    }
                }
            }
        }
    }
}