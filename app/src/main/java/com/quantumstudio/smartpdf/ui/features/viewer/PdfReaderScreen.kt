package com.quantumstudio.smartpdf.ui.features.viewer

import PdfInfoDialog
import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.barteksc.pdfviewer.PDFView
import com.quantumstudio.smartpdf.ui.components.BottomActionIcon
import com.quantumstudio.smartpdf.ui.components.BrightnessSliderLayout
import com.quantumstudio.smartpdf.ui.features.main.MainViewModel
import com.quantumstudio.smartpdf.util.CommonUtils.sharePdf
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun PdfReaderScreen(uri: Uri, onBack: () -> Unit, viewModel: MainViewModel) {
    val context = LocalContext.current
    val activity = context as? Activity

    // --- 1. 状态管理 ---
    var isUiVisible by rememberSaveable { mutableStateOf(true) }
    var isNightMode by rememberSaveable { mutableStateOf(false) }
    var showMenu by remember { mutableStateOf(false) }
    var showBrightnessSlider by rememberSaveable { mutableStateOf(false) }
    var showJumpLayout by rememberSaveable { mutableStateOf(false) }
    var showInfoDialog by remember { mutableStateOf(false) }

    var lastLoadedUri by remember { mutableStateOf<Uri?>(null) }
    var currentPage by rememberSaveable { mutableStateOf(0) }
    var totalPages by rememberSaveable { mutableStateOf(0) }
    var scrollProgress by remember { mutableStateOf(0f) }
    var pdfViewInstance by remember { mutableStateOf<PDFView?>(null) }

    // --- 2. 数据绑定 ---
    val pdfFiles by viewModel.pdfFiles.collectAsState()
    val currentPdf = remember(pdfFiles, uri) { pdfFiles.find { it.path == uri.path } }
    val isFavorite = currentPdf?.isFavorite ?: false

    // 页码指示器动画逻辑
    var isPageIndicatorVisible by remember { mutableStateOf(false) }
    var scrollSignal by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(scrollSignal) {
        if (totalPages > 0) {
            isPageIndicatorVisible = true
            delay(1500)
            isPageIndicatorVisible = false
        }
    }

    BackHandler { onBack() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // --- 3. PDF 内容层 ---
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
                        .defaultPage(currentPage)
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
                        .onLoad { viewModel.markAsRead(uri.path ?: "") }
                        .load()
                    lastLoadedUri = uri
                } else {
                    pdfView.setNightMode(isNightMode)
                }
            }
        )

        // --- 4. 平滑滚动指示器 (品牌红) ---
        val screenHeightPx =
            with(LocalDensity.current) { LocalConfiguration.current.screenHeightDp.dp.toPx() }
        // 1. 在这里定义 dragRange，确保它在下面的所有块中都可用
        val dragRange = screenHeightPx * 0.7f
        Box(modifier = Modifier.align(Alignment.CenterEnd)) {// 2. 使用 scrollProgress 计算实时偏移
            val yOffsetPx = scrollProgress * dragRange
            AnimatedVisibility(
                visible = isPageIndicatorVisible,
                enter = fadeIn() + slideInHorizontally { it },
                exit = fadeOut() + slideOutHorizontally { it }) {
                val dragRange = screenHeightPx * 0.7f
                val yOffsetPx = scrollProgress * dragRange
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, (yOffsetPx - dragRange / 2).roundToInt()) }
                        .size(50.dp, 32.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.9f),
                            RoundedCornerShape(topStart = 16.dp, bottomStart = 16.dp)
                        )
                        .draggable(
                            orientation = Orientation.Vertical,
                            state = rememberDraggableState { delta ->
                                // ✨ 这里现在可以安全引用外部的 dragRange 了
                                val newProgress =
                                    (scrollProgress + delta / dragRange).coerceIn(0f, 1f)
                                scrollProgress = newProgress

                                // 计算目标页码并跳转
                                val targetPage = (newProgress * (totalPages - 1)).roundToInt()
                                if (targetPage != currentPage) {
                                    pdfViewInstance?.jumpTo(targetPage)
                                }
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        "${currentPage + 1}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
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
                    IconButton(onClick = onBack) {
                        Icon(
                            Icons.Default.ArrowBackIosNew,
                            "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Text(
                        uri.lastPathSegment ?: "Reader",
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            Icons.Default.MoreVert,
                            "More",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false },
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ) {
                        DropdownMenuItem(
                            text = { Text("Info") },
                            leadingIcon = { Icon(Icons.Default.Info, null) },
                            onClick = { showMenu = false; showInfoDialog = true })
                        DropdownMenuItem(
                            text = { Text("Share") },
                            leadingIcon = { Icon(Icons.Default.Share, null) },
                            onClick = {
                                currentPdf?.let { sharePdf(context, it) }; showMenu = false
                            })
                    }
                }
            }
        }

        // --- 6. 底部操作面板 (核心整合) ---
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
                    if (showBrightnessSlider) {
                        BrightnessSliderLayout(activity)
                    } else if (showJumpLayout) {
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
                                    if (it.requestedOrientation == ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE) ActivityInfo.SCREEN_ORIENTATION_PORTRAIT else ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                            }
                        }
                        BottomActionIcon(if (isNightMode) Icons.Default.LightMode else Icons.Default.DarkMode) {
                            isNightMode = !isNightMode; lastLoadedUri = null
                        }

                        // 亮度开关
                        BottomActionIcon(
                            icon = Icons.Default.WbSunny,
                            tint = if (showBrightnessSlider) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) { showBrightnessSlider = !showBrightnessSlider; showJumpLayout = false }

                        // 跳转开关 (语义化图标更新)
                        BottomActionIcon(
                            icon = Icons.Default.FindInPage,
                            tint = if (showJumpLayout) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) { showJumpLayout = !showJumpLayout; showBrightnessSlider = false }
                        
                        // 收藏按钮
                        BottomActionIcon(
                            icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            tint = if (isFavorite) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        ) { viewModel.toggleFavorite(uri.path ?: "") }
                    }
                }
            }
        }

        if (showInfoDialog && currentPdf != null) PdfInfoDialog(currentPdf) {
            showInfoDialog = false
        }
    }
}

@Composable
fun JumpPageLayout(currentPage: Int, totalPages: Int, onConfirm: (Int) -> Unit) {
    var textValue by remember { mutableStateOf((currentPage + 1).toString()) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "Total: $totalPages",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        OutlinedTextField(
            value = textValue,
            onValueChange = { if (it.all { c -> c.isDigit() }) textValue = it },
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
                .height(50.dp),
            shape = RoundedCornerShape(25.dp),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )
        )
        Button(
            onClick = {
                textValue.toIntOrNull()?.minus(1)
                    ?.let { if (it in 0 until totalPages) onConfirm(it) }
            },
            shape = RoundedCornerShape(20.dp),
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("Go", color = Color.White)
        }
    }
}