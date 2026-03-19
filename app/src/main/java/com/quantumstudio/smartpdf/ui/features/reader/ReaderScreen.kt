package com.quantumstudio.smartpdf.ui.features.reader

import PdfInfoDialog
import android.app.Activity
import android.net.Uri
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.quantumstudio.smartpdf.util.CommonUtils.sharePdf
import com.quantumstudio.smartpdf.util.ShortcutUtils
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@RequiresApi(Build.VERSION_CODES.N_MR1)
@Composable
fun PdfReaderScreen(
    uri: Uri,
    onBack: () -> Unit,
    viewModel: ReaderViewModel
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // 1. 路径清洗
    val pdfPath = remember(uri) {
        val rawPath = uri.path ?: ""
        if (rawPath.startsWith("/file")) rawPath.substring(5) else rawPath
    }

    // 2. 观察数据（pdfFiles 仅用于大列表同步，currentReadingPdf 用于当前阅读）
    val currentPdf = viewModel.currentReadingPdf

    LaunchedEffect(pdfPath) {
        viewModel.loadPdfForReader(pdfPath)
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
                            viewModel.markAsRead(currentPdf)
                        }
                        .load()
                    lastLoadedUri = uri
                } else {
                    pdfView.setNightMode(isNightMode)
                }
            }
        )

        // 进度滑块层
        PdfScrollbarThumb(
            modifier = Modifier.align(Alignment.CenterEnd), // 明确指定在右侧居中
            isVisible = isPageIndicatorVisible,
            currentPage = currentPage,
            scrollProgress = scrollProgress,
            onScrollDelta = { delta ->
                val newProgress = (scrollProgress + delta).coerceIn(0f, 1f)
                scrollProgress = newProgress
                val targetPage = (newProgress * (totalPages - 1)).roundToInt()
                if (targetPage != currentPage) pdfViewInstance?.jumpTo(targetPage)
            }
        )

        ReaderTopBar(
            isUiVisible = isUiVisible,
            title = uri.lastPathSegment ?: "Reader",
            onBack = onBack,
            onInfoClick = { showInfoDialog = true },
            onAddToHomeClick = {
                currentPdf?.let { pdf ->
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        ShortcutUtils.addPdfToHomeScreen(context, pdf)
                    } else {
                        android.widget.Toast.makeText(
                            context,
                            "Not supported",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            },
            onShareClick = {
                currentPdf?.let { sharePdf(context, it) }
            }
        )

        // 底层控制面板
        ReaderBottomPanel(
            modifier = Modifier.align(Alignment.BottomCenter),
            isVisible = isUiVisible,
            isFavorite = isFavorite,
            isNightMode = isNightMode,
            currentPage = currentPage,
            totalPages = totalPages,
            showBrightnessSlider = showBrightnessSlider,
            showJumpLayout = showJumpLayout,
            onToggleBrightness = {
                showBrightnessSlider = !showBrightnessSlider
                showJumpLayout = false
            },
            onToggleJump = {
                showJumpLayout = !showJumpLayout
                showBrightnessSlider = false
            },
            onToggleNightMode = { isNightMode = !isNightMode; lastLoadedUri = null },
            onToggleFavorite = { viewModel.toggleFavorite(currentPdf) },
            onRotationClick = { /* 屏幕旋转逻辑 */ },
            onJumpToPage = { target -> pdfViewInstance?.jumpTo(target); showJumpLayout = false },
            activity = activity
        )

        if (showInfoDialog) {
            PdfInfoDialog(currentPdf) { showInfoDialog = false }
        }
    }
}