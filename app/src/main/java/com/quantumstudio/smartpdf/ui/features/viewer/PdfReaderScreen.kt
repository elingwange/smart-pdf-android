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
import androidx.compose.material.icons.filled.Favorite
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
import androidx.compose.runtime.collectAsState
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
import com.quantumstudio.smartpdf.ui.features.main.MainViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt


@Composable
fun PdfReaderScreen(uri: Uri, onBack: () -> Unit, viewModel: MainViewModel) {
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

    // --- 状态绑定 ---
    // 观察全局 PDF 列表
    val pdfFiles by viewModel.pdfFiles.collectAsState()

    // 自动初始化并实时跟随数据源：根据当前 URI 的路径查找其收藏状态
    val isFavorite = remember(pdfFiles, uri) {
        pdfFiles.find { it.path == uri.path }?.isFavorite ?: false
    }

    // 控制显隐逻辑：页码指示器
    var isPageIndicatorVisible by remember { mutableStateOf(false) }
    // 修改：让初始值也作为信号的一部分，或者手动触发一次
    var scrollSignal by remember { mutableStateOf(System.currentTimeMillis()) }

    // 只要 scrollSignal 变动，就重新计时
    LaunchedEffect(scrollSignal) {
        // 只有总页数已知时才执行，避免初始加载时的空白闪烁
        if (totalPages > 0) {
            isPageIndicatorVisible = true
            delay(1300) // 你设置的 1.3 秒
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
                            // 增加：页码改变也算滑动信号，触发倒计时
                            scrollSignal = System.currentTimeMillis()
                        }
                        .onPageScroll { _, _ ->
                            // 滑动位移触发信号
                            scrollSignal = System.currentTimeMillis()
                        }
                        .onLoad {
                            // 文件加载成功，记录为最近阅读
                            viewModel.markAsRead(uri.path ?: "")
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
                                // 这里的 UI 显示是正确的，因为重组会更新它们
                                icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                label = if (isFavorite) "Remove from favorites" else "Add to favorites",
                                tint = if (isFavorite) Color(0xFF9999FF) else Color.White
                            ) {
                                val path = uri.path ?: ""
                                if (path.isNotEmpty()) {
                                    // 逻辑修复：先保存当前状态，用于判断提示语
                                    val willBeFavorite = !isFavorite

                                    // 1. 调用 ViewModel 更新（内存+数据库）
                                    viewModel.toggleFavorite(path)

                                    // 2. 弹出提示：使用 willBeFavorite 确保提示语与动作一致
                                    val message =
                                        if (willBeFavorite) "Added to favorites" else "Removed from favorites"
                                    android.widget.Toast.makeText(
                                        context,
                                        message,
                                        android.widget.Toast.LENGTH_SHORT
                                    ).show()
                                }
                                showMenu = false
                            }
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