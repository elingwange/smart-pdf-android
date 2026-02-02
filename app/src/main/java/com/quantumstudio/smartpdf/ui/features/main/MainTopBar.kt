package com.quantumstudio.smartpdf.ui.features.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sort
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(currentRoute: String?) {
    // 根据路由判断显示哪种 TopBar
    when (currentRoute) {
        "settings" -> {
            TopAppBar(
                title = { Text("Settings", color = Color.White, fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF121212)
                )
            )
        }

        else -> {
            // 默认显示主页的 TopBar（包含 Smart PDF 标题和分类 Tab）
            HomeTopBar()
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
                IconButton(onClick = { /* 打开文件夹逻辑 */ }) {
                    Icon(Icons.Default.FolderOpen, null, tint = Color.White)
                }
                IconButton(onClick = { /* 排序逻辑 */ }) {
                    Icon(Icons.Default.Sort, null, tint = Color.White)
                }
                IconButton(onClick = { /* 搜索逻辑 */ }) {
                    Icon(Icons.Default.Search, null, tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )

        // 分类 Tab 栏
        val tabs = listOf("ALL", "PDF", "WORD", "EXCEL", "PPT")
        var selectedTabIndex by remember { mutableStateOf(0) }

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
                            text = title,
                            color = if (selectedTabIndex == index) Color.White else Color.Gray,
                            fontWeight = if (selectedTabIndex == index) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
    }
}