package com.quantumstudio.smartpdf.ui.features.main.components

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import com.quantumstudio.smartpdf.ui.theme.PdfRed


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    currentPage: Int,
    onSearchClick: () -> Unit,
    onSortClick: () -> Unit,
    onOpenFileClick: () -> Unit
) {
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
                containerColor = MaterialTheme.colorScheme.surfaceContainer
            )
        )
    } else {
        // 0, 1, 2 都是主文件相关的页面，显示带有 Logo 和操作按钮的 HomeTopBar
        HomeTopBar(currentPage, onSearchClick, onSortClick, onOpenFileClick)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    currentPage: Int,
    onSearchClick: () -> Unit,
    onSortClick: () -> Unit,
    onOpenFileClick: () -> Unit
) {
    val context = LocalContext.current

    Column(modifier = Modifier.background(MaterialTheme.colorScheme.surfaceContainer)) {
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
                        color = PdfRed,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            actions = {
                // 这里可以根据 currentPage 做微调，比如在“最近”页面不显示排序
                IconButton(onClick = { onOpenFileClick() }) {
                    Icon(Icons.Default.FolderOpen, null, tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onSortClick) {
                    Icon(Icons.Default.Sort, null, tint = MaterialTheme.colorScheme.onSurface)
                }
                IconButton(onClick = onSearchClick) {
                    Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.onSurface)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Transparent,
                titleContentColor = Color.White,
                actionIconContentColor = Color.White
            )
        )
    }
}