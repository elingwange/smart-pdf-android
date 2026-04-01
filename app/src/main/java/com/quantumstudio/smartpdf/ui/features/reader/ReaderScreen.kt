package com.quantumstudio.smartpdf.ui.features.reader

import PdfInfoDialog
import ReaderBottomPanel
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.barteksc.pdfviewer.PDFView
import com.quantumstudio.smartpdf.ui.features.reader.components.PdfScrollbarThumb
import com.quantumstudio.smartpdf.ui.features.reader.components.ReaderTopBar
import com.quantumstudio.smartpdf.util.CommonUtils
import com.quantumstudio.smartpdf.util.CommonUtils.sharePdf
import com.quantumstudio.smartpdf.util.ShortcutUtils
import kotlinx.coroutines.delay
import kotlinx.coroutines.yield
import kotlin.math.roundToInt


@RequiresApi(Build.VERSION_CODES.N_MR1)
@Composable
fun PdfReaderScreen(
    uriString: String?, // 注意：这里直接收 String 类型的路由参数
    uri: Uri,
    onBack: () -> Unit,
    viewModel: ReaderViewModel,
    // 状态管家由外部注入或默认创建，确保唯一性
    uiState: ReaderUiState = rememberReaderUiState(),
    pdfState: PdfViewState = rememberPdfViewState()
) {
    val context = LocalContext.current
    val activity = context as? Activity

    // 1. 订阅 ViewModel 中的加载状态和数据库中的 PDF 实体
    val loadStatus by viewModel.loadStatus.collectAsStateWithLifecycle()
    val currentPdf = viewModel.currentReadingPdf

    // 关键：进入页面后立即根据路由参数加载
    LaunchedEffect(uriString) {
        // 先解码，还原回原始 content://... 格式
        val decodedSource = Uri.decode(uriString)
        viewModel.loadPdf(decodedSource)
    }

    // 3. 处理系统级副作用：返回键与亮度
    BackHandler { onBack() }

    DisposableEffect(Unit) {
        onDispose {
            activity?.let {
                val lp = it.window.attributes
                lp.screenBrightness = -1f // 恢复系统自动亮度
                it.window.attributes = lp
            }
        }
    }

    LaunchedEffect(uiState.currentBrightness) {
        if (uiState.activePanel == ReaderPanel.Brightness) {
            activity?.let {
                val lp = it.window.attributes
                lp.screenBrightness = uiState.currentBrightness
                it.window.attributes = lp
            }
        }
    }

    LaunchedEffect(pdfState.scrollSignal) {
        if (pdfState.totalPages > 0) {
            uiState.isPageIndicatorVisible = true
            delay(200)
            yield()
            uiState.isPageIndicatorVisible = false
        }
    }

    // 4. UI 渲染主逻辑
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        when (val status = loadStatus) {
            is PdfLoadStatus.Loading -> {
                // 加载中：显示进度条，不挂载 PDFView 避免 NaN
                LoadingPlaceholder()
            }

            is PdfLoadStatus.Error -> {
                // 错误处理
                ErrorPlaceholder(status.message, onBack)
            }

            is PdfLoadStatus.Success -> {
                val tempFile = status.file

                // 成功后：触发数据库记录补录/查询
                LaunchedEffect(tempFile.absolutePath) {
                    viewModel.loadPdfForReader(tempFile.absolutePath)
                }

                // 只有数据库记录也准备好了，才渲染核心内容
                if (currentPdf != null) {
                    // 核心渲染层
                    PdfContentLayer(
                        file = tempFile,
                        uri = uri,
                        currentPdf = currentPdf,
                        uiState = uiState,
                        pdfState = pdfState,
                        viewModel = viewModel,
                        onBack = onBack,
                        activity = activity
                    )
                } else {
                    LoadingPlaceholder()
                }
            }

            else -> { /* Idle 状态不处理 */
            }
        }
    }
}

@Composable
private fun PdfContentLayer(
    file: java.io.File,
    uri: Uri,
    currentPdf: com.quantumstudio.smartpdf.data.model.PdfFile,
    uiState: ReaderUiState,
    pdfState: PdfViewState,
    viewModel: ReaderViewModel,
    onBack: () -> Unit,
    activity: Activity?
) {
    val context = LocalContext.current

    // 自动保存进度逻辑保持不变
    LaunchedEffect(pdfState.currentPage) {
        if (!pdfState.isFirstLoad) {
            delay(100)
            viewModel.updateProgress(file.absolutePath, pdfState.currentPage)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // --- 视图层 (AndroidView) ---
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PDFView(ctx, null).apply {
                    setOnClickListener { uiState.toggleUi() }
                    // ✨ 关键修复 1：在 factory 中就建立引用，确保实例稳定
                    pdfState.pdfView = this
                }
            },
            update = { pdfView ->
                // ✨ 关键修复 2：显式读取状态，确保 update 块订阅了这些变量的变化
                val nightMode = uiState.isNightMode
                val currentFilePath = file.absolutePath

                if (pdfState.lastLoadedFilePath != currentFilePath) {
                    // 首次加载或文件切换
                    pdfView.fromFile(file)
                        .nightMode(nightMode)
                        .defaultPage(currentPdf.currentPage)
                        .fitEachPage(true)
                        .pageFling(true)
                        .onPageChange { p, c -> pdfState.updatePage(p, c) }
                        // ✨ 关键修复 3：补回丢失的滑动监听，否则 scrollProgress 永远是 0，指示器不走
                        .onPageScroll { page, offset ->
                            pdfState.updateScroll(page, offset)
                        }
                        .onLoad {
                            pdfState.isFirstLoad = false
                            pdfState.lastLoadedFilePath = currentFilePath
                            pdfView.zoomTo(1f)
                        }
                        .load()
                } else {
                    // ✨ 关键修复 4：解决切换主题不即时生效的问题
                    // PDFView 内部对 nightMode 的修改有时需要重新加载或强制刷新
                    pdfView.setNightMode(nightMode)
                    // 强制 PDFView 重绘滤镜层
                    pdfView.invalidate()
                }
            }
        )

        // --- 悬浮 UI 组件 ---
        // ✨ 关键修复 5：只要有页数就显示，确保滚动条逻辑正确关联
        if (pdfState.totalPages > 0) {

            PdfScrollbarThumb(
                modifier = Modifier.align(Alignment.CenterEnd),
                isVisible = uiState.isPageIndicatorVisible,
                currentPage = pdfState.currentPage,
                // 这里 pdfState.scrollProgress 的变化现在会由于 onPageScroll 的补回而生效
                scrollProgress = pdfState.scrollProgress,
                onScrollDelta = { delta ->
                    pdfState.pdfView?.let { view ->
                        // delta 向上滑是负的，向下滑是正的
                        val newProgress = (pdfState.scrollProgress + delta).coerceIn(0f, 1f)

                        // 更新状态，驱动 UI 重组
                        pdfState.scrollProgress = newProgress

                        // 计算目标页码
                        val targetPage = (newProgress * (pdfState.totalPages - 1)).roundToInt()

                        // 只有页码真的变了才跳转，减少抖动
                        if (targetPage != pdfState.currentPage) {
                            view.jumpTo(targetPage)
                        }
                    }
                }
            )

            ReaderTopBar(
                isUiVisible = uiState.isUiVisible,
                title = currentPdf.name, // 使用数据库里的名字更准确
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
                onToggleFavorite = { viewModel.toggleFavorite(currentPdf) },
                onRotationClick = {
                    CommonUtils.toggleScreenOrientation(activity)
                    // 旋转后清空标识，强制 AndroidView 重新 load 以适配横竖屏布局
                    pdfState.lastLoadedFilePath = null
                },
                activity = activity
            )
        }

        if (uiState.showInfoDialog) {
            PdfInfoDialog(currentPdf) { uiState.showInfoDialog = false }
        }
    }
}

@Composable
private fun LoadingPlaceholder() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun ErrorPlaceholder(message: String, onBack: () -> Unit) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        androidx.compose.material3.Text(
            text = message,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.background(MaterialTheme.colorScheme.errorContainer)
        )
        // 此处建议加个返回按钮
    }
}