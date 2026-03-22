package com.quantumstudio.smartpdf.ui.navigation

import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
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

            PdfReaderScreen(
                uri = uri,
                viewModel = readerViewModel,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
