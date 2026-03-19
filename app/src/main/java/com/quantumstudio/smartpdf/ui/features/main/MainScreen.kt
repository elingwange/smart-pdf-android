package com.quantumstudio.smartpdf.ui.features.main

import PdfInfoDialog
import SearchScreen
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
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.ui.components.MenuAction
import com.quantumstudio.smartpdf.ui.components.PdfDeleteDialog
import com.quantumstudio.smartpdf.ui.components.PdfListItem
import com.quantumstudio.smartpdf.ui.components.PdfRenameDialog
import com.quantumstudio.smartpdf.ui.components.PermissionGuideScreen
import com.quantumstudio.smartpdf.ui.components.SortByDialog
import com.quantumstudio.smartpdf.ui.features.settings.SettingsScreen
import com.quantumstudio.smartpdf.util.CommonUtils
import com.quantumstudio.smartpdf.util.CommonUtils.sharePdf
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToReader: (Uri) -> Unit // ✨ 关键：接收外部导航回调
) {
    val context = LocalContext.current
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    // 控制搜索界面的状态
    var isSearching by remember { mutableStateOf(false) }

    // 控制排序对话框状态
    var showSortDialog by remember { mutableStateOf(false) }
    val currentField by viewModel.sortField.collectAsState()
    val currentOrder by viewModel.sortOrder.collectAsState()

    // 权限与扫描逻辑
    LaunchedEffect(viewModel.hasFileAccess) {
        if (viewModel.hasFileAccess) viewModel.scanPdfs(context)
    }

    // 渲染排序对话框
    if (showSortDialog) {
        SortByDialog(
            currentField = currentField,
            currentOrder = currentOrder,
            onDismiss = { showSortDialog = false },
            onConfirm = { field, order ->
                viewModel.updateSortConfig(field, order)
                showSortDialog = false
            }
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            topBar = {
                if (viewModel.hasFileAccess) MainTopBar(
                    currentPage = pagerState.currentPage,
                    onSearchClick = { isSearching = true },
                    onSortClick = { showSortDialog = true }
                )
            },
            bottomBar = {
                if (viewModel.hasFileAccess) {
                    AppBottomNavigation(
                        currentPage = pagerState.currentPage,
                        onTabSelected = { index ->
                            scope.launch { pagerState.scrollToPage(index) }
                        }
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
                    PermissionGuideScreen(onGrantClick = {
                        CommonUtils.requestAllFilesAccess(context)
                    })
                } else {
                    HorizontalPager(
                        state = pagerState,
                        userScrollEnabled = false,
                        beyondViewportPageCount = 3
                    ) { pageIndex ->
                        // ✨ 统一点击逻辑：调用导航回调
                        val onFileClick: (Uri) -> Unit = { uri -> onNavigateToReader(uri) }

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

        // 搜索图层
        if (isSearching) {
            SearchScreen(
                viewModel = viewModel,
                onBack = {
                    isSearching = false
                    viewModel.onQueryChange("")
                },
                onFileClick = { pdf ->
                    isSearching = false
                    viewModel.onQueryChange("")
                    // ✨ 搜索结果也通过导航跳转
                    onNavigateToReader(Uri.fromFile(File(pdf.path)))
                }
            )
        }
    }
}

@Composable
fun PdfListContent(
    files: List<PdfFile>,
    viewModel: MainViewModel,
    onFileClick: (Uri) -> Unit
) {
    val context = LocalContext.current
    var selectedPdfForInfo by remember { mutableStateOf<PdfFile?>(null) }
    var pdfToDelete by remember { mutableStateOf<PdfFile?>(null) }
    var pdfToRename by remember { mutableStateOf<PdfFile?>(null) }

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
                        onClick = { onFileClick(Uri.fromFile(File(pdf.path))) },
                        onMenuAction = { action ->
                            when (action) {
                                is MenuAction.Info -> selectedPdfForInfo = pdf
                                is MenuAction.Favorite -> viewModel.toggleFavorite(pdf)
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

        // 弹窗逻辑
        selectedPdfForInfo?.let { pdf ->
            PdfInfoDialog(pdf = pdf, onDismiss = { selectedPdfForInfo = null })
        }

        pdfToDelete?.let { pdf ->
            PdfDeleteDialog(
                fileName = pdf.name,
                onDismiss = { pdfToDelete = null },
                onConfirm = {
                    viewModel.deleteFile(pdf, context)
                    pdfToDelete = null
                }
            )
        }

        pdfToRename?.let { pdf ->
            PdfRenameDialog(
                currentName = pdf.name,
                onDismiss = { pdfToRename = null },
                onConfirm = { newName ->
                    viewModel.renameFile(pdf, newName)
                    pdfToRename = null
                }
            )
        }
    }
}

// --- Tabs 实现保持简洁，统一向下传递回调 ---

@Composable
fun AllFilesTab(viewModel: MainViewModel, onFileClick: (Uri) -> Unit) {
    val files by viewModel.sortedPdfFiles.collectAsState()
    PdfListContent(files, viewModel, onFileClick)
}

@Composable
fun FavoriteFilesTab(viewModel: MainViewModel, onFileClick: (Uri) -> Unit) {
    val files by viewModel.sortedPdfFiles.collectAsState()
    val favoriteFiles = remember(files) { files.filter { it.isFavorite } }
    PdfListContent(favoriteFiles, viewModel, onFileClick)
}

@Composable
fun RecentFilesTab(viewModel: MainViewModel, onFileClick: (Uri) -> Unit) {
    val files by viewModel.allPdfsFlow.collectAsState()
    val recentFiles = remember(files) {
        files.filter { it.lastReadTime > 0 }.sortedByDescending { it.lastReadTime }
    }
    PdfListContent(recentFiles, viewModel, onFileClick)
}

@Composable
fun AppBottomNavigation(currentPage: Int, onTabSelected: (Int) -> Unit) {
    val items = listOf(
        "Home" to Icons.Default.Home,
        "Fav" to Icons.Default.Favorite,
        "Recent" to Icons.Default.History,
        "Set" to Icons.Default.Settings
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        items.forEachIndexed { index, (label, icon) ->
            NavigationBarItem(
                selected = currentPage == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(icon, null) },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}