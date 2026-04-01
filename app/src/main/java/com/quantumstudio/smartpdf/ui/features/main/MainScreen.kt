package com.quantumstudio.smartpdf.ui.features.main

import android.net.Uri
import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import com.quantumstudio.smartpdf.ui.common.UiEvent
import com.quantumstudio.smartpdf.ui.features.main.components.AppBottomNavigation
import com.quantumstudio.smartpdf.ui.features.main.components.MainPager
import com.quantumstudio.smartpdf.ui.features.main.components.MainSearchOverlay
import com.quantumstudio.smartpdf.ui.features.main.components.MainTopBar
import com.quantumstudio.smartpdf.ui.features.main.components.SortByDialog
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    navController: NavController,
    onNavigateToReader: (Uri) -> Unit
) {
    // 1. 定义接收器
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { it ->
            Log.d("---ELog", "设置页面拿到文件: $it")
            val encodedUri = Uri.encode(it.toString())
            navController.navigate("reader/$encodedUri")
        }
    }


    val pendingUri by viewModel.pendingReaderUri.collectAsStateWithLifecycle()
    LaunchedEffect(pendingUri) {
        val uri = pendingUri ?: return@LaunchedEffect

        // 确保 NavHost 准备好
        val graph = try {
            navController.graph
        } catch (e: Exception) {
            null
        }

        // 注意：这里要匹配 "reader/{pdfUri}" 的模式，或者检查其前缀
        if (graph != null) {
            val encodedUri = Uri.encode(uri.toString())
            val targetRoute = "reader/$encodedUri"

            // 关键：导航到你定义的路由
            navController.navigate(targetRoute) {
                launchSingleTop = true
            }
            viewModel.consumePendingUri()
        }
    }


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
        if (viewModel.hasFileAccess) {
            // 等待界面进入交互状态后再开始重度扫描
            delay(600)
            viewModel.scanPdfs(context)
        }
    }

    // 监听 ViewModel 发出的 UI 事件
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
//        SortByBottomSheet(
//            currentField = currentField,
//            currentOrder = currentOrder,
//            onDismiss = { showSortDialog = false },
//            onConfirm = { field, order ->
//                viewModel.updateSortConfig(field, order)
//                // 在 BottomSheet 中，通常点击选项后直接关闭
//                showSortDialog = false
//            }
//        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackBarHostState) },
            topBar = {
                if (viewModel.hasFileAccess) MainTopBar(
                    currentPage = pagerState.currentPage,
                    onSearchClick = { isSearching = true },
                    onSortClick = { showSortDialog = true },
                    // ✅ 2. 这里的“接线”是关键！
                    onOpenFileClick = {
                        Log.d("---ELog", "TopBar 点击了打开文件")
                        filePickerLauncher.launch("application/pdf")
                    }
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
                navController = navController,
                onNavigateToReader = onNavigateToReader
            )
        }

        if (isSearching) {
            BackHandler {
                isSearching = false
            }
            MainSearchOverlay(
                isSearching = isSearching,
                viewModel = viewModel,
                onClose = { isSearching = false },
                onNavigateToReader = onNavigateToReader
            )
        }
    }
}
