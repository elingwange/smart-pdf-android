package com.quantumstudio.smartpdf.ui.features.main

import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.quantumstudio.smartpdf.ui.common.UiEvent
import com.quantumstudio.smartpdf.ui.components.SortByDialog
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onNavigateToReader: (Uri) -> Unit
) {
    val snackBarHostState = remember { SnackbarHostState() }
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

    // ✨ 新增：监听 ViewModel 发出的 UI 事件
    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackBar -> {
                    scope.launch {
                        val result = snackBarHostState.showSnackbar(
                            message = event.message,
                            actionLabel = event.actionLabel,
                            duration = SnackbarDuration.Short
                        )
                        // 判断用户是否点击了“撤销”
                        if (result == SnackbarResult.ActionPerformed) {
                            event.onAction?.invoke()
                        }
                    }
                }
            }
        }
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
            snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
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
            MainPager(
                modifier = Modifier.padding(innerPadding),
                viewModel = viewModel,
                pagerState = pagerState,
                onNavigateToReader = onNavigateToReader
            )
        }

        MainSearchOverlay(
            isSearching = isSearching,
            viewModel = viewModel,
            onClose = { isSearching = false },
            onNavigateToReader = onNavigateToReader
        )
    }
}
