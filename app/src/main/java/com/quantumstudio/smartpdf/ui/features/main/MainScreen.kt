package com.quantumstudio.smartpdf.ui.features.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.ui.components.PdfListItem
import com.quantumstudio.smartpdf.ui.components.PermissionGuideScreen
import com.quantumstudio.smartpdf.ui.features.settings.SettingsScreen
import com.quantumstudio.smartpdf.util.CommonUtils
import kotlinx.coroutines.launch

@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val context = LocalContext.current
    // 1. 定义 PagerState 替代 NavController 进行 Tab 切换
    val pagerState = rememberPagerState(pageCount = { 4 })
    val scope = rememberCoroutineScope()

    // 权限与数据监听逻辑保持不变
    LaunchedEffect(Unit) { viewModel.checkPermission(context) }
    LaunchedEffect(viewModel.hasFileAccess) {
        if (viewModel.hasFileAccess) viewModel.scanPdfs(context)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            if (viewModel.hasFileAccess) {
                // 将 pagerState 传进去，以便根据页面改变 TopBar 内容
                MainTopBar(currentPage = pagerState.currentPage)
            }
        },
        bottomBar = {
            if (viewModel.hasFileAccess) {
                AppBottomNavigation(
                    currentPage = pagerState.currentPage,
                    onTabSelected = { index ->
                        // 瞬间跳转，不带动画以追求极致速度
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
                PermissionGuideScreen(onGrantClick = { CommonUtils.requestAllFilesAccess(context) })
            } else {
                // 2. 核心容器：HorizontalPager
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = false, // 禁用手势滑动，确保点击切换的确定性
                    beyondViewportPageCount = 3 // 预加载所有页面，实现“秒开”
                ) { pageIndex ->
                    // 3. 根据页面索引分发不同的内容
                    when (pageIndex) {
                        0 -> AllFilesTab(viewModel)
                        1 -> RecentFilesTab(viewModel)
                        2 -> FavoriteFilesTab(viewModel)
                        3 -> SettingsScreen(viewModel = viewModel)
                    }
                }
            }
        }
    }
}

@Composable
fun AppBottomNavigation(currentPage: Int, onTabSelected: (Int) -> Unit) {
    val items = listOf(
        "All Files" to Icons.Default.Home,
        "Recent" to Icons.Default.History,
        "Bookmark" to Icons.Default.Bookmark,
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

                    // 💡 使用我们新定义的 onSurfaceVariant，它亮度刚好，不刺眼也不模糊
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,

                    indicatorColor = Color.Transparent // 保持你的透明偏好
                )
            )
        }
    }
}


@Composable
fun PdfListContent(
    files: List<PdfFile>,
    viewModel: MainViewModel
) {
    if (files.isEmpty()) {
        // 通用空状态
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("No PDF files found", color = Color.Gray)
        }
    } else {
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            items(
                items = files,
                key = { it.path } // 使用路径作为 Key，利于 Compose 跨 Tab 识别并复用组件
            ) { pdf ->
                PdfListItem(
                    pdf = pdf,
                    onClick = { /* TODO: 跳转阅读器 */ },
                    onMoreClick = { /* TODO: 显示操作菜单 */ }
                )
            }
        }
    }
}

// 三个 Tab 只是对数据的不同“视图”
@Composable
fun AllFilesTab(viewModel: MainViewModel) {
    val files by viewModel.pdfFiles.collectAsState()
    PdfListContent(files, viewModel)
}

@Composable
fun RecentFilesTab(viewModel: MainViewModel) {
    val files by viewModel.pdfFiles.collectAsState()
    // 假设你有 isRecent 过滤逻辑
    val recentFiles = remember(files) { files.filter { it.isRecent } }
    PdfListContent(recentFiles, viewModel)
}

@Composable
fun FavoriteFilesTab(viewModel: MainViewModel) {
    val files by viewModel.pdfFiles.collectAsState()
    val favoriteFiles = remember(files) { files.filter { it.isFavorite } }
    PdfListContent(favoriteFiles, viewModel)
}