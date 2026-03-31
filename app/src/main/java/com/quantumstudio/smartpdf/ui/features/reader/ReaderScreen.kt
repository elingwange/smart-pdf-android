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

    // 自动保存进度：页码变动后防抖写入
    LaunchedEffect(pdfState.currentPage) {
        if (!pdfState.isFirstLoad) {
            delay(1000)
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
                    pdfState.pdfView = this
                }
            },
            update = { pdfView ->
                // 关键：对比文件路径而不是 URI，因为缓存文件名是带时间戳的
                if (pdfState.lastLoadedFilePath != file.absolutePath) {
                    pdfView.fromFile(file)
                        .nightMode(uiState.isNightMode)
                        .defaultPage(currentPdf.currentPage) // 从数据库恢复进度
                        .fitEachPage(true)
                        .pageFling(true)
                        .onPageChange { p, c -> pdfState.updatePage(p, c) }
                        .onLoad {
                            pdfState.isFirstLoad = false
                            pdfState.lastLoadedFilePath = file.absolutePath
                            pdfView.zoomTo(1f)
                            viewModel.markAsRead(currentPdf)
                        }
                        .load()
                } else {
                    pdfView.setNightMode(uiState.isNightMode)
                }
            }
        )

        // --- 悬浮 UI 组件 (仅在有页数数据时显示，防止 NaN) ---
        if (pdfState.totalPages > 0) {
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
                title = uri.lastPathSegment ?: "SmartPDF Reader",
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
                    // 旋转后清空路径标识，强制 AndroidView 重新触发适配布局的 load()
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