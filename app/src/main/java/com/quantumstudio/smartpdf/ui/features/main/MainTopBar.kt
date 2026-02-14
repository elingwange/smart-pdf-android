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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(currentPage: Int) { // 修改这里：接收 Int 类型的索引
    // 根据索引判断显示哪种 TopBar
    // 假设索引 3 是设置页
    if (currentPage == 3) {
        TopAppBar(
            title = {
                Text(
                    "Settings",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = MaterialTheme.colorScheme.background
            )
        )
    } else {
        // 0, 1, 2 都是主文件相关的页面，显示带有 Logo 和操作按钮的 HomeTopBar
        HomeTopBar(currentPage)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(currentPage: Int) {
    Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
        TopAppBar(
            title = {
                Row {
                    Text(
                        "Smart ",
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "PDF",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            actions = {
                // 这里可以根据 currentPage 做微调，比如在“最近”页面不显示排序
                IconButton(onClick = { /* ... */ }) {
                    Icon(Icons.Default.FolderOpen, null, tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { /* ... */ }) {
                    Icon(Icons.Default.Sort, null, tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = { /* ... */ }) {
                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )

        // 注意：如果你希望下方的 ScrollableTabRow 跟着底部的 Tab 联动，
        // 你可以直接使用传进来的 currentPage
//        val tabs = listOf("ALL", "PDF", "DOC", "PPT") // 这里是你原来的分类逻辑
//
//        ScrollableTabRow(
//            selectedTabIndex = 0, // 暂时固定，或者根据业务逻辑处理
//            containerColor = Color.Transparent,
//            edgePadding = 16.dp,
//            divider = {},
//            indicator = { tabPositions ->
//                TabRowDefaults.SecondaryIndicator(
//                    modifier = Modifier.tabIndicatorOffset(tabPositions[0]),
//                    color = Color.Red
//                )
//            }
//        ) {
//            tabs.forEachIndexed { index, title ->
//                androidx.compose.material3.Tab(
//                    selected = index == 0,
//                    onClick = { /* 处理分类切换 */ },
//                    text = {
//                        Text(
//                            text = title,
//                            color = if (index == 0) Color.White else Color.Gray,
//                            fontWeight = if (index == 0) FontWeight.Bold else FontWeight.Normal
//                        )
//                    }
//                )
//            }
//        }
    }
}