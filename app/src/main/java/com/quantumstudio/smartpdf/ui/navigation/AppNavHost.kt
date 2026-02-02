package com.quantumstudio.smartpdf.ui.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.quantumstudio.smartpdf.ui.features.main.FilesScreen
import com.quantumstudio.smartpdf.ui.features.settings.SettingsScreen
import com.quantumstudio.smartpdf.ui.features.main.MainViewModel
import com.quantumstudio.smartpdf.util.FileUtils

@Composable
fun AppNavHost(
    navController: NavHostController,
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    NavHost(
        navController = navController,
        startDestination = "all_files",
        modifier = modifier,
        enterTransition = { fadeIn(tween(300)) },
        exitTransition = { fadeOut(tween(300)) }
    ) {
        composable("all_files") {
            FilesScreen(
                pdfFiles = viewModel.pdfFiles,
                onOpenPdf = { pdf -> FileUtils.openPdf(context, pdf) },
                onRefresh = { viewModel.scanPdfs(context) }
            )
        }
        composable("recent") { /* 最近页面 */ }
        composable("bookmark") { /* 书签页面 */ }
        composable("settings") { SettingsScreen() }
    }
}