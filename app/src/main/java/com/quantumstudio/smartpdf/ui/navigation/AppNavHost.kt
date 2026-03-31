package com.quantumstudio.smartpdf.ui.navigation

import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.quantumstudio.smartpdf.ui.features.main.MainScreen
import com.quantumstudio.smartpdf.ui.features.main.MainViewModel
import com.quantumstudio.smartpdf.ui.features.reader.PdfReaderScreen
import com.quantumstudio.smartpdf.ui.features.reader.ReaderViewModel


@RequiresApi(Build.VERSION_CODES.N_MR1)
@Composable
fun AppNavHost(
    navController: NavHostController,
    mainViewModel: MainViewModel,
    readerViewModel: ReaderViewModel
) {
    NavHost(
        navController = navController,
        startDestination = "main" // 默认进入首页
    ) {
        // 首页
        composable("main") {
            MainScreen(
                viewModel = mainViewModel,
                navController,
                onNavigateToReader = { uri ->
                    val encodedUri = Uri.encode(uri.toString())
                    navController.navigate("reader/$encodedUri")
                }
            )
        }

        // 阅读器页面
        composable(
            route = "reader/{pdfUri}",
            arguments = listOf(navArgument("pdfUri") { type = NavType.StringType })
        ) { backStackEntry ->
            val uriString = backStackEntry.arguments?.getString("pdfUri")
            val uri = Uri.parse(Uri.decode(uriString))

            Log.d("--ELog", "Navigated with URI: $uri")

            // ✨ 关键点：当进入这个 Composable 时，立即触发加载逻辑
            LaunchedEffect(uri) {
                readerViewModel.loadPdf(uriString ?: "")
            }

            PdfReaderScreen(
                uriString = uriString,
                uri = uri,
                viewModel = readerViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
