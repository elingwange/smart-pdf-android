package com.quantumstudio.smartpdf.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import java.io.File

// 1. 定义底部 Tab 数据类
data class BottomTabItem(
    val label: String,
    val icon: ImageVector,
    val screen: @Composable () -> Unit
)

@Composable
fun BottomTabBar(
    importedFiles: List<File>,
    onSelectPdf: () -> Unit,
    onOpenPdf: (File) -> Unit
) {
    // 当前选中的 Tab 索引
    var selectedTab by remember { mutableStateOf(0) }

    // 定义 Tab 列表
    val tabs = listOf(
        BottomTabItem("Files", Icons.Default.List) {
            FilesScreen(
                importedFiles = importedFiles,
                onSelectPdf = onSelectPdf,
                onOpenPdf = onOpenPdf
            )
        },
        BottomTabItem("Mate", Icons.Default.Face) { AIMateScreen() },
        BottomTabItem("Settings", Icons.Default.Settings) { SettingsScreen() }
    )

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        icon = { Icon(tab.icon, contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { paddingValues ->
        // 显示对应页面
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            tabs[selectedTab].screen()
        }
    }
}


