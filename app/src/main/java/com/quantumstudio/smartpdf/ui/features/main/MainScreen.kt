package com.quantumstudio.smartpdf.ui.features.main

import PdfInfoDialog
import PdfReaderScreen
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.ui.components.MenuAction
import com.quantumstudio.smartpdf.ui.components.PdfDeleteDialog
import com.quantumstudio.smartpdf.ui.components.PdfListItem
import com.quantumstudio.smartpdf.ui.components.PdfRenameDialog
import com.quantumstudio.smartpdf.ui.components.PermissionGuideScreen
import com.quantumstudio.smartpdf.ui.features.settings.SettingsScreen
import com.quantumstudio.smartpdf.util.CommonUtils
import com.quantumstudio.smartpdf.util.CommonUtils.sharePdf
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    // 追踪当前打开的 PDF
    var activePdfUri by remember { mutableStateOf<Uri?>(null) }

    // 权限逻辑
    LaunchedEffect(Unit) { viewModel.checkPermission(context) }
    LaunchedEffect(viewModel.hasFileAccess) {
        if (viewModel.hasFileAccess) viewModel.scanPdfs(context)
    }

    Scaffold(
        topBar = { if (viewModel.hasFileAccess) MainTopBar(currentPage = pagerState.currentPage) },
        bottomBar = {
            if (viewModel.hasFileAccess) {
                AppBottomNavigation(
                    currentPage = pagerState.currentPage,
                    onTabSelected = { index -> scope.launch { pagerState.scrollToPage(index) } }
                )
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (!viewModel.hasFileAccess) {
                PermissionGuideScreen(onGrantClick = { CommonUtils.requestAllFilesAccess(context) })
            } else {
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false,
                    beyondViewportPageCount = 3
                ) { pageIndex ->
                    // 关键点 1：在这里定义点击后的逻辑
                    val onFileClick: (Uri) -> Unit = { uri -> activePdfUri = uri }

                    when (pageIndex) {
                        0 -> AllFilesTab(viewModel, onFileClick)
                        1 -> FavoriteFilesTab(viewModel, onFileClick)
                        2 -> RecentFilesTab(viewModel, onFileClick)
                        3 -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }

    // PDF 预览层（盖在最上面）
    activePdfUri?.let { uri ->
        PdfReaderScreen(uri = uri, onBack = { activePdfUri = null }, viewModel = viewModel)
    }
}

// 关键点 2：PdfListContent 必须声明接收这个函数参数
@Composable
fun PdfListContent(
    files: List<PdfFile>,
    viewModel: MainViewModel,
    onFileClick: (Uri) -> Unit
) {
    val context = LocalContext.current
    // 状态定义（正确）
    var selectedPdfForInfo by remember { mutableStateOf<PdfFile?>(null) }
    // 新增：用于删除确认的状态
    var pdfToDelete by remember { mutableStateOf<PdfFile?>(null) }

    var pdfToRename by remember { mutableStateOf<PdfFile?>(null) }


    // 修改：增加 Box 以便 Dialog 能够正确弹出
    Box(modifier = Modifier.fillMaxSize()) {
        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No PDF files found", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = files, key = { it.path }) { pdf ->
                    PdfListItem(
                        pdf = pdf,
                        onClick = { onFileClick(Uri.fromFile(java.io.File(pdf.path))) },
                        onMenuAction = { action ->
                            when (action) {
                                is MenuAction.Info -> selectedPdfForInfo = pdf // 赋值（正确）
                                is MenuAction.Favorite -> viewModel.toggleFavorite(pdf.path)
                                is MenuAction.Rename -> pdfToRename = pdf
                                is MenuAction.Delete -> pdfToDelete = pdf
                                is MenuAction.Share -> sharePdf(context, pdf)
                                else -> {}
                            }
                        }
                    )
                }
            }
        }

        // 关键修复：添加这一段代码，让 Dialog 真正渲染出来
        selectedPdfForInfo?.let { pdf ->
            PdfInfoDialog(
                pdf = pdf,
                onDismiss = { selectedPdfForInfo = null }
            )
        }

        // 添加删除弹窗
        pdfToDelete?.let { pdf ->
            PdfDeleteDialog(
                fileName = pdf.name,
                onDismiss = { pdfToDelete = null },
                onConfirm = {
                    viewModel.deleteFile(pdf, context) // 执行删除
                    pdfToDelete = null
                }
            )
        }

        pdfToRename?.let { pdf ->
            PdfRenameDialog(
                currentName = pdf.name,
                onDismiss = { pdfToRename = null },
                onConfirm = { newName ->
                    viewModel.renameFile(pdf, newName, context)
                    pdfToRename = null
                }
            )
        }
    }
}

// 关键点 4：所有的 Tab 都要接收并向下传递 onFileClick
@Composable
fun AllFilesTab(viewModel: MainViewModel, onFileClick: (Uri) -> Unit) {
    val files by viewModel.pdfFiles.collectAsState()
    PdfListContent(files, viewModel, onFileClick)
}

@Composable
fun RecentFilesTab(viewModel: MainViewModel, onFileClick: (Uri) -> Unit) {
    val files by viewModel.pdfFiles.collectAsState()

    // 过滤出阅读时间大于 0 的文件，并按时间倒序排列
    val recentFiles = remember(files) {
        files.filter { it.lastReadTime > 0 }
            .sortedByDescending { it.lastReadTime }
    }

    PdfListContent(recentFiles, viewModel, onFileClick)
}

@Composable
fun FavoriteFilesTab(viewModel: MainViewModel, onFileClick: (Uri) -> Unit) {
    val files by viewModel.pdfFiles.collectAsState()
    val favoriteFiles = remember(files) { files.filter { it.isFavorite } }
    PdfListContent(favoriteFiles, viewModel, onFileClick)
}

@Composable
fun AppBottomNavigation(currentPage: Int, onTabSelected: (Int) -> Unit) {
    val items = listOf(
        "All Files" to Icons.Default.Home,
        "Favorite" to Icons.Default.Favorite,
        "Recent" to Icons.Default.History,
        "Settings" to Icons.Default.Settings
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.surface) {
        items.forEachIndexed { index, (label, icon) ->
            NavigationBarItem(
                selected = currentPage == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    // onSurfaceVariant亮度刚好，不刺眼也不模糊
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    indicatorColor = Color.Transparent // 保持透明偏好
                )
            )
        }
    }
}
