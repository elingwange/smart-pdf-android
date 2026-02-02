package com.quantumstudio.smartpdf.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.quantumstudio.smartpdf.pdf.viewer.PDFViewerActivity
import com.quantumstudio.smartpdf.ui.viewmodel.MainViewModel
import java.io.File


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {// 自动注入 ViewModel
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val scope = rememberCoroutineScope()

    val context = LocalContext.current // 获取当前 Context
    // 首次进入自动检查
    LaunchedEffect(Unit) {
        viewModel.checkPermission(context)
    }
    // 监听权限状态，一旦获得权限就自动扫描
    LaunchedEffect(viewModel.hasFileAccess) {
        if (viewModel.hasFileAccess) {
            viewModel.scanPdfs(context)
        }
    }

    // 1. 文件选择器逻辑
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let {
            // 这里处理获取到的 PDF 路径，例如保存到数据库
            println("Selected file: $it")
        }
    }

    // 2. 底部菜单状态
    var showSheet by remember { mutableStateOf(false) }
    // 假设选中的文件
    // var selectedPdf by remember { mutableStateOf<PdfFile?>(null) }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            if (currentRoute == "settings") {
                TopAppBar(
                    title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF121212))
                )
            } else {
                HomeTopBar() // 包含 PDF Reader 标题和 TabRow
            }
        },
        bottomBar = {
            AppBottomNavigation(navController, currentRoute)
        },
        floatingActionButton = {
            // 仅在非设置页面显示红色添加按钮
            if (currentRoute != "settings") {
                FloatingActionButton(
                    onClick = {
                        // 点击调起系统文件选择，过滤 PDF
                        filePickerLauncher.launch(arrayOf("application/pdf"))
                    },
                    containerColor = Color(0xFFE91E63), // 接近截图的红色
                    shape = CircleShape,
                    modifier = Modifier.padding(bottom = 8.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add File", tint = Color.White)
                }
            }
        }
    ) { innerPadding ->
        // 2. 内容区域：根据权限状态切换
        Box(modifier = Modifier.padding(innerPadding)) {
            if (!viewModel.hasFileAccess) {
                // 显示引导页
                PermissionGuideScreen(onGrantClick = {
                    requestAllFilesAccess(context)
                })
            } else {
                NavHost(
                    navController = navController,
                    startDestination = "all_files",
                    modifier = Modifier.fillMaxSize(),
                    // 彻底禁用缩放，只保留纯粹的淡入淡出
                    enterTransition = {
                        fadeIn(animationSpec = tween(300))
                    },
                    exitTransition = {
                        fadeOut(animationSpec = tween(300))
                    },
                    popEnterTransition = { fadeIn(animationSpec = tween(300)) },
                    popExitTransition = { fadeOut(animationSpec = tween(300)) }
                ) {
                    composable("all_files") {
                        // 这里调用你之前的 PdfListScreen()
                        Text("All Files List Content", color = Color.White)
                        FilesScreen2(
                            pdfFiles = viewModel.pdfFiles,
                            onOpenPdf = { pdf ->
                                // 直接在这里处理跳转逻辑
                                val intent = Intent(context, PDFViewerActivity::class.java).apply {
                                    putExtra("pdf_uri", Uri.fromFile(File(pdf.path)))
                                }
                                context.startActivity(intent)
                            },
                            onRefresh = { viewModel.scanPdfs(context) }
                        )
                    }
                    composable("recent") { Text("Recent Files", color = Color.White) }
                    composable("bookmark") { Text("Bookmarks", color = Color.White) }
                    composable("settings") { SettingsScreen() }
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar() {
    Column(modifier = Modifier.background(Color(0xFF121212))) {
        TopAppBar(
            title = {
                Row {
                    Text("Smart ", color = Color.White, fontWeight = FontWeight.Bold)
                    Text("PDF", color = Color.Red, fontWeight = FontWeight.Bold)
                }
            },
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Default.FolderOpen, null, tint = Color.White)
                }
                IconButton(onClick = {}) { Icon(Icons.Default.Sort, null, tint = Color.White) }
                IconButton(onClick = {}) { Icon(Icons.Default.Search, null, tint = Color.White) }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )

        /*
        // --- Tab 部分保持不变 ---
        val tabs = listOf("ALL", "PDF", "WORD", "EXCEL", "PPT")
        var selectedTabIndex by remember { mutableStateOf(0) } // 建议增加状态管理

        ScrollableTabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color.Transparent,
            edgePadding = 16.dp,
            divider = {},
            indicator = { tabPositions ->
                if (selectedTabIndex < tabPositions.size) {
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                        color = Color.Red
                    )
                }
            }
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = {
                        Text(
                            title,
                            color = if (selectedTabIndex == index) Color.White else Color.Gray,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
         */
    }
}

@Composable
fun AppBottomNavigation(navController: androidx.navigation.NavController, currentRoute: String?) {
    val items = listOf(
        Triple("all_files", "All Files", Icons.Default.Home),
        Triple("recent", "Recent", Icons.Default.History),
        Triple("bookmark", "Bookmark", Icons.Default.Bookmark),
        Triple("settings", "Settings", Icons.Default.Settings)
    )

    NavigationBar(containerColor = Color(0xFF1C1C1C)) {
        items.forEach { (route, label, icon) ->
            val isSelected = currentRoute == route
            NavigationBarItem(
                selected = isSelected,
                onClick = {
                    navController.navigate(route) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(icon, contentDescription = null) },
                label = { Text(label, fontSize = 10.sp) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = Color.Red,
                    selectedTextColor = Color.Red,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun PermissionGuideScreen(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF121212))
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 顶部图标：文件夹+安全锁的意象
        Box(
            modifier = Modifier
                .size(120.dp)
                .background(Color(0xFF1E1E1E), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.FolderOpen,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = Color.Red
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 标题
        Text(
            text = "需要文件访问权限",
            color = Color.White,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(16.dp))

        // 描述文字
        Text(
            text = "为了能够扫描并阅读您手机中的 PDF 文件，Smart PDF 需要获得“所有文件访问”权限。这仅用于文件管理功能。",
            color = Color.Gray,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(48.dp))

        // 授权按钮
        Button(
            onClick = onGrantClick,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("去授权", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
        }
    }
}

// 你的权限请求函数，放在工具类或 MainScreen 文件末尾
private fun requestAllFilesAccess(context: Context) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
            data = Uri.parse("package:${context.packageName}")
        }
        context.startActivity(intent)
    } else {
        // Android 11 以下请求常规权限，这里可以使用 rememberLauncherForActivityResult
    }
}