package com.quantumstudio.smartpdf.ui.features.reader

import PdfInfoDialog
import ReaderBottomPanel
import android.app.Activity
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.quantumstudio.smartpdf.ui.features.reader.components.PdfScrollbarThumb
import com.quantumstudio.smartpdf.ui.features.reader.components.ReaderTopBar
import com.quantumstudio.smartpdf.util.CommonUtils
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

    // 1. 核心业务数据
    val pdfPath = remember(uri) {
        val rawPath = uri.path ?: ""
        if (rawPath.startsWith("/file")) rawPath.substring(5) else rawPath
    }
    val currentPdf = viewModel.currentReadingPdf

    // 2. 状态大管家实例化
    val uiState = rememberReaderUiState()
    val pdfState = rememberPdfViewState()

    // ✨ 核心逻辑：退出时恢复系统亮度
    DisposableEffect(Unit) {
        // 当组件进入屏幕时（可选操作，比如锁定屏幕常亮）
        onDispose {
            // 当组件从屏幕卸载（退出阅读页面）时执行
            activity?.let {
                val lp = it.window.attributes
                lp.screenBrightness = -1f // 🚀 关键：恢复为 -1f，交还控制权给系统
                it.window.attributes = lp
            }
        }
    }
    // ✨ 新增：状态恢复后的物理同步
    LaunchedEffect(uiState.currentBrightness) {
        // 只有当亮度不是默认初始值，或者处于手动调节模式时，才同步给系统
        if (uiState.currentBrightness != 0.5f || uiState.activePanel == ReaderPanel.Brightness) {
            activity?.let {
                val lp = it.window.attributes
                lp.screenBrightness = uiState.currentBrightness
                it.window.attributes = lp
            }
        }
    }

    // 3. 初始加载逻辑
    LaunchedEffect(pdfPath) { viewModel.loadPdfForReader(pdfPath) }

    if (currentPdf == null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    // 4. 状态同步逻辑
    // 首次进入：从数据库恢复页码
    LaunchedEffect(currentPdf.currentPage) {
        if (pdfState.isFirstLoad && currentPdf.currentPage > 0) {
            pdfState.currentPage = currentPdf.currentPage
        }
    }

    // 自动保存：页码变动后防抖写入数据库
    LaunchedEffect(pdfState.currentPage) {
        if (!pdfState.isFirstLoad) {
            delay(1000)
            viewModel.updateProgress(pdfPath, pdfState.currentPage)
        }
    }

    // 滚动指示器自动消失逻辑
    LaunchedEffect(pdfState.scrollSignal) {
        if (pdfState.totalPages > 0) {
            uiState.isPageIndicatorVisible = true
            delay(2000)
            uiState.isPageIndicatorVisible = false
        }
    }

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 视图层 ---
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PDFView(ctx, null).apply {
                    setOnClickListener { uiState.toggleUi() }
                    pdfState.pdfView = this
                }
            },
            update = { pdfView ->
                if (pdfState.lastLoadedUri != uri) {
                    pdfView.fromUri(uri)
                        .nightMode(uiState.isNightMode)
                        .defaultPage(pdfState.currentPage)
                        // ✨ 关键修复：强制每一页适配当前屏幕尺寸，防止旋转后的比例错误
                        .fitEachPage(true)
                        .pageFling(true)    // 丝滑翻页
                        .onPageChange { p, c -> pdfState.updatePage(p, c) }
                        .onPageScroll { p, o -> pdfState.updateScroll(p, o) }
                        .onLoad {
                            pdfState.isFirstLoad = false
                            pdfState.lastLoadedUri = uri
                            // ✨ 额外保险：加载完成后，手动重置缩放级别为 1.0
                            pdfView.zoomTo(1f)
                            viewModel.markAsRead(currentPdf)
                        }
                        .load()
                } else {
                    pdfView.setNightMode(uiState.isNightMode)
                }
            }
        )

        // --- UI 组件层 ---
        PdfScrollbarThumb(
            modifier = Modifier.align(Alignment.CenterEnd),
            isVisible = uiState.isPageIndicatorVisible,
            currentPage = pdfState.currentPage,
            scrollProgress = pdfState.scrollProgress,
            onScrollDelta = { delta ->
                val newProgress = (pdfState.scrollProgress + delta).coerceIn(0f, 1f)
                pdfState.scrollProgress = newProgress
                val targetPage = (newProgress * (pdfState.totalPages - 1)).roundToInt()
                if (targetPage != pdfState.currentPage) pdfState.pdfView?.jumpTo(targetPage)
            }
        )

        ReaderTopBar(
            isUiVisible = uiState.isUiVisible,
            title = uri.lastPathSegment ?: "Reader",
            onBack = onBack,
            onInfoClick = { uiState.showInfoDialog = true },
            onAddToHomeClick = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    ShortcutUtils.addPdfToHomeScreen(context, currentPdf)
                }
            },
            onShareClick = { sharePdf(context, currentPdf) }
        )

        ReaderBottomPanel(
            modifier = Modifier.align(Alignment.BottomCenter),
            uiState = uiState,
            pdfState = pdfState,
            isFavorite = currentPdf.isFavorite,
            onToggleFavorite = {
                Log.d("--------", "onToggleFavorite()")
                viewModel.toggleFavorite(currentPdf)
            },
            onRotationClick = {
                CommonUtils.toggleScreenOrientation(activity)
                pdfState.lastLoadedUri = null // 触发重载适配布局
            },
            activity = activity
        )

        if (uiState.showInfoDialog) {
            PdfInfoDialog(currentPdf) { uiState.showInfoDialog = false }
        }
    }
}