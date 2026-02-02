package com.quantumstudio.smartpdf.ui.features.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.quantumstudio.smartpdf.ui.components.PermissionGuideScreen
import com.quantumstudio.smartpdf.ui.navigation.AppNavHost
import com.quantumstudio.smartpdf.util.CommonUtils.requestAllFilesAccess


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: MainViewModel = viewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route
    val context = LocalContext.current

    // 生命周期与状态监听
    LaunchedEffect(Unit) { viewModel.checkPermission(context) }
    LaunchedEffect(viewModel.hasFileAccess) {
        if (viewModel.hasFileAccess) viewModel.scanPdfs(context)
    }

    Scaffold(
        containerColor = Color(0xFF121212),
        topBar = {
            if (viewModel.hasFileAccess) MainTopBar(currentRoute)
        },
        bottomBar = {
            if (viewModel.hasFileAccess) AppBottomNavigation(navController, currentRoute)
        },
        floatingActionButton = {
            if (viewModel.hasFileAccess && currentRoute != "settings") {
                //MainFab { /* 以后可以接文件选择器 */ }
            }
        }
    ) { innerPadding ->
        // 使用统一的 Box 处理 Padding，避免双重缩进
        Box(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            if (!viewModel.hasFileAccess) {
                PermissionGuideScreen(onGrantClick = { requestAllFilesAccess(context) })
            } else {
                AppNavHost(navController = navController, viewModel = viewModel)
            }
        }
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
