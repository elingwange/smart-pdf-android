package com.quantumstudio.smartpdf.ui.features.main.components

import SearchScreen
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.quantumstudio.smartpdf.R
import com.quantumstudio.smartpdf.ui.features.main.MainViewModel
import com.quantumstudio.smartpdf.ui.features.settings.SettingsScreen
import com.quantumstudio.smartpdf.ui.features.settings.components.PermissionGuideScreen
import com.quantumstudio.smartpdf.ui.theme.PdfRed
import com.quantumstudio.smartpdf.util.CommonUtils
import java.io.File

@Composable
fun MainPager(
    modifier: Modifier,
    viewModel: MainViewModel,
    pagerState: PagerState,
    navController: NavController,
    onNavigateToReader: (Uri) -> Unit
) {
    val context = LocalContext.current
    Box(modifier = modifier.fillMaxSize()) {
        if (!viewModel.hasFileAccess) {
            PermissionGuideScreen(onGrantClick = { CommonUtils.requestAllFilesAccess(context) })
        } else {
            HorizontalPager(state = pagerState, userScrollEnabled = false) { pageIndex ->
                when (pageIndex) {
                    0 -> AllFilesTab(viewModel, onNavigateToReader)
                    1 -> FavoriteFilesTab(viewModel, onNavigateToReader)
                    2 -> RecentFilesTab(viewModel, onNavigateToReader)
                    3 -> SettingsScreen(navController)
                }
            }
        }
    }
}

@Composable
fun AppBottomNavigation(currentPage: Int, onTabSelected: (Int) -> Unit) {
    val items = listOf(
        stringResource(R.string.home) to Icons.Default.Home,
        stringResource(R.string.favorites) to Icons.Default.Favorite,
        stringResource(R.string.recents) to Icons.Default.History,
        stringResource(R.string.settings) to Icons.Default.Settings
    )

    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
        items.forEachIndexed { index, (label, icon) ->
            NavigationBarItem(
                selected = currentPage == index,
                onClick = { onTabSelected(index) },
                icon = { Icon(icon, null) },
                label = { Text(label, fontSize = 13.sp) },
                colors = NavigationBarItemDefaults.colors(
//                    selectedIconColor = MaterialTheme.colorScheme.primary,
//                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    selectedIconColor = PdfRed,
                    selectedTextColor = PdfRed,
                    indicatorColor = Color.Transparent
                )
            )
        }
    }
}

@Composable
fun MainSearchOverlay(
    isSearching: Boolean,
    viewModel: MainViewModel,
    onClose: () -> Unit,
    onNavigateToReader: (Uri) -> Unit
) {
    AnimatedVisibility(
        visible = isSearching,
        enter = expandVertically(animationSpec = tween(300)) + fadeIn(),
        exit = shrinkVertically(animationSpec = tween(300)) + fadeOut()
    ) {
        SearchScreen(
            viewModel = viewModel,
            onBack = {
                onClose()
                viewModel.onQueryChange("")
            },
            onFileClick = { pdf ->
                onClose()
                viewModel.onQueryChange("")
                onNavigateToReader(Uri.fromFile(File(pdf.path)))
            }
        )
    }
}