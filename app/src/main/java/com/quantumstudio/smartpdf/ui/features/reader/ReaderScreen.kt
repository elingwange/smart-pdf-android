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
import com.quantumstudio.smartpdf.util.CommonUtils.uriToFile
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


    // 监听打印信号
    LaunchedEffect(uiState.isPrinting) {
        if (uiState.isPrinting) {
            val file = uriToFile(context, uri)
            // 调用系统打印
            CommonUtils.printPdf(context, file, currentPdf?.name)

            // 💡 关键：调起系统打印界面后，立刻把状态重置回 false
            // 否则下次重组或恢复状态时可能会重复调起打印
            uiState.isPrinting = false
        }
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
                    pdfState.pdfView = this

                    // 第一次加载
                    fromFile(file)
                        .nightMode(uiState.isNightMode)
                        .defaultPage(currentPdf.currentPage)
                        .fitEachPage(true) // 关键：开启单页自适应
                        .pageFling(true)
                        .onPageChange { p, c -> pdfState.updatePage(p, c) }
                        .onPageScroll { p, o -> pdfState.updateScroll(p, o) }
                        .onLoad {
                            pdfState.isFirstLoad = false
                            pdfState.lastLoadedFilePath = file.absolutePath
                        }
                        .load()
                }
            },
            update = { pdfView ->
                // 1. 处理夜间模式
                if (pdfView.isNightMode != uiState.isNightMode) {
                    pdfView.setNightMode(uiState.isNightMode)
                    pdfView.invalidate()
                }

                // 2. 修复旋转后的缩放 Bug
                if (pdfState.lastLoadedFilePath == null) {
                    // ✨ 关键防御代码：检查 PDFView 内部的页面数量是否大于 0
                    // 如果 pageCount 为 0，说明 PdfFile 对象还是 null，此时调用 zoom 会崩溃
                    if (pdfView.pageCount > 0) {
                        // 使用非动画版的 zoom，避免触发 AnimationManager
                        pdfView.zoomTo(1f)

                        // 如果非要用带动画的重置，先确保 view 已经绘制
                        pdfView.post {
                            try {
                                if (pdfView.pageCount > 0) {
                                    pdfView.resetZoomWithAnimation()
                                }
                            } catch (e: Exception) {
                                // 防御性捕获，防止极速旋转导致的意外
                            }
                        }
                        pdfState.lastLoadedFilePath = file.absolutePath
                    }
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
                onPrintClick = { uiState.isPrinting = true },
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