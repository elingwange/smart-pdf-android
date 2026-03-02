package com.quantumstudio.smartpdf.ui.features.viewer

import PdfInfoDialog
import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
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
import androidx.compose.material.icons.filled.AddHome
import androidx.compose.material.icons.filled.ArrowBackIosNew
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FindInPage
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.ScreenRotation
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.quantumstudio.smartpdf.ui.components.BottomActionIcon
import com.quantumstudio.smartpdf.ui.components.BrightnessSliderLayout
import com.quantumstudio.smartpdf.ui.components.JumpPageLayout
import com.quantumstudio.smartpdf.ui.features.main.MainViewModel
import com.quantumstudio.smartpdf.util.CommonUtils.sharePdf
import com.quantumstudio.smartpdf.util.ShortcutUtils
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.N_MR1)
@Composable
fun PdfReaderScreen(uri: Uri, onBack: () -> Unit, viewModel: MainViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity

// 1. 路径清洗
    val pdfPath = remember(uri) {
        val rawPath = uri.path ?: ""
        if (rawPath.startsWith("/file")) rawPath.substring(5) else rawPath
    }

    // 2. 观察数据（pdfFiles 仅用于大列表同步，currentReadingPdf 用于当前阅读）
    val pdfFiles by viewModel.pdfFiles.collectAsState()
    val currentPdf = viewModel.currentReadingPdf

    // 3. ✨ 核心加载逻辑：合并 LaunchedEffect
    LaunchedEffect(pdfPath) {
        viewModel.loadPdfForReader(pdfPath)
        // 加载后的 Log 更有参考意义
        android.util.Log.d("---PDF_TRACE", "单点查询已触发，路径: $pdfPath")
    }

    // 4. ✨ 屏障逻辑：没拿到数据库对象前，不渲染后续 UI
    // 这样 initialPage 就绝对不会因为“数据还没到”而变成默认值 0
    if (currentPdf == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return // 阻塞后续逻辑，直到数据库返回
    }

    // 5. 状态派生（此时 currentPdf 绝对不为空）
    val isFavorite = currentPdf.isFavorite
    val initialPage = currentPdf.currentPage

    // --- 2. 状态管理 ---
    var isFirstLoad by remember { mutableStateOf(true) }
    var currentPage by rememberSaveable { mutableStateOf(0) }
    var totalPages by rememberSaveable { mutableStateOf(0) }
    var scrollProgress by remember { mutableStateOf(0f) }
    var pdfViewInstance by remember { mutableStateOf<PDFView?>(null) }

    // 关键修复：当数据库数据加载完成后，同步 initialPage 到内存 currentPage
    LaunchedEffect(initialPage) {
        if (isFirstLoad && initialPage > 0) {
            currentPage = initialPage
            // 同步计算 scrollProgress，确保指示器位置正确
            if (totalPages > 1) {
                scrollProgress = initialPage.toFloat() / (totalPages - 1)
            }
        }
    }

    // 保存逻辑：防抖保存
    LaunchedEffect(currentPage) {
        if (!isFirstLoad) {
            delay(1000) // 停止翻页 1 秒后写入数据库
            viewModel.updateProgress(pdfPath, currentPage)
        }
    }

    // UI 显示状态
    var isUiVisible by rememberSaveable { mutableStateOf(true) }
    var isNightMode by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showBrightnessSlider by rememberSaveable { mutableStateOf(false) }
    var showJumpLayout by rememberSaveable { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    var lastLoadedUri by remember { mutableStateOf<Uri?>(null) }
    var isPageIndicatorVisible by remember { mutableStateOf(false) }
    var scrollSignal by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(scrollSignal) {
        if (totalPages > 0) {
            isPageIndicatorVisible = true
            delay(2000)
            isPageIndicatorVisible = false
        }
    }

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 3. PDF 视图层 ---
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PDFView(ctx, null).apply {
                    setOnClickListener {
                        isUiVisible = !isUiVisible
                        if (!isUiVisible) {
                            showBrightnessSlider = false
                            showJumpLayout = false
                        }
                    }
                    pdfViewInstance = this
                }
            },
            update = { pdfView ->
                if (lastLoadedUri != uri) {
                    pdfView.fromUri(uri)
                        .nightMode(isNightMode)
                        .defaultPage(if (isFirstLoad) initialPage else currentPage)
                        .onPageChange { page, count ->
                            currentPage = page
                            totalPages = count
                        }
                        .onPageScroll { page, positionOffset ->
                            if (totalPages > 1) {
                                scrollProgress =
                                    (page + positionOffset) / (totalPages - 1).toFloat()
                            }
                            scrollSignal = System.currentTimeMillis()
                        }
                        .onLoad {
                            isFirstLoad = false
                            viewModel.markAsRead(pdfPath)
                        }
                        .load()
                    lastLoadedUri = uri
                } else {
                    pdfView.setNightMode(isNightMode)
                }
            }
        )

        // --- 4. 侧边进度指示器 ---
        val screenHeightPx =
            with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
        val dragRange = screenHeightPx * 0.7f

        Box(modifier = Modifier.align(Alignment.CenterEnd)) {
            AnimatedVisibility(
                visible = isPageIndicatorVisible,
                enter = fadeIn() + slideInHorizontally { it },
                exit = fadeOut() + slideOutHorizontally { it }
            ) {
                // 根据 scrollProgress 实时计算 Y 轴偏移
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
                            state = rememberDraggableState { delta ->
                                val newProgress =
                                    (scrollProgress + delta / dragRange).coerceIn(0f, 1f)
                                scrollProgress = newProgress
                                val targetPage = (newProgress * (totalPages - 1)).roundToInt()
                                if (targetPage != currentPage) {
                                    pdfViewInstance?.jumpTo(targetPage)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text("${currentPage + 1}", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }

        // --- 5. 顶部导航栏 ---
        AnimatedVisibility(
            visible = isUiVisible,
            enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
            modifier = Modifier.align(Alignment.TopCenter)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                tonalElevation = 4.dp
            ) {
                Row(
                    modifier = Modifier
                        .statusBarsPadding()
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBackIosNew, "Back") }
                    Text(
                        uri.lastPathSegment ?: "Reader",
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    // ✨ 修改这里：创建一个 Box 作为锚点
                    Box {

                        IconButton(onClick = { showMenu = true }) {
                            Icon(
                                Icons.Default.MoreVert,
                                "More"
                            )
                        }
                        DropdownMenu(expanded = showMenu, onDismissRequest = { showMenu = false }) {
                            DropdownMenuItem(
                                text = { Text("Info") },
                                leadingIcon = { Icon(Icons.Default.Info, null) },
                                onClick = { showMenu = false; showInfoDialog = true }
                            )
                            DropdownMenuItem(
                                text = { Text("Add Shortcut") },
                                leadingIcon = { Icon(Icons.Default.AddHome, null) },
                                onClick = {
                                    showMenu = false
                                    currentPdf?.let { pdf ->
                                        // ✨ 修复：增加 API 等级检查
                                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                            ShortcutUtils.addPdfToHomeScreen(context, pdf)
                                        } else {
                                            // 可选：给旧版本用户的提示
                                            android.widget.Toast.makeText(
                                                context,
                                                "Your Android version does not support this feature",
                                                android.widget.Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Share") },
                                leadingIcon = { Icon(Icons.Default.Share, null) },
                                onClick = {
                                    currentPdf?.let { sharePdf(context, it) }; showMenu = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // --- 6. 底部操作面板 ---
        AnimatedVisibility(
            visible = isUiVisible,
            enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
            exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
            modifier = Modifier.align(Alignment.BottomCenter)
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
                    if (showBrightnessSlider) BrightnessSliderLayout(activity)
                    else if (showJumpLayout) {
                        JumpPageLayout(currentPage, totalPages) { targetPage ->
                            pdfViewInstance?.jumpTo(targetPage)
                            showJumpLayout = false
                        }
                    }

                    Row(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        BottomActionIcon(Icons.Default.ScreenRotation) {
                            activity?.let {
                                lastLoadedUri = null
                                it.requestedOrientation =
                                    if (it.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
                                        ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                        }
                        BottomActionIcon(if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode) {
                            isNightMode = !isNightMode; lastLoadedUri = null
                        }// 亮度按钮
                        BottomActionIcon(
                            icon = Icons.Default.WbSunny,
                            tint = if (showBrightnessSlider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = {
                                showBrightnessSlider = !showBrightnessSlider
                                showJumpLayout = false
                            }
                        )

                        // 跳转按钮
                        BottomActionIcon(
                            icon = Icons.Default.FindInPage,
                            tint = if (showJumpLayout) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = {
                                showJumpLayout = !showJumpLayout
                                showBrightnessSlider = false
                            }
                        )

                        // 收藏按钮
                        BottomActionIcon(
                            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            onClick = { viewModel.toggleFavorite(pdfPath) }
                        )
                    }
                }
            }
        }

        if (showInfoDialog && currentPdf != null) {
            PdfInfoDialog(currentPdf) { showInfoDialog = false }
        }
    }
}