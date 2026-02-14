package com.quantumstudio.smartpdf.ui.navigation

//@Composable
//fun AppNavHost(
//    navController: NavHostController,
//    viewModel: MainViewModel,
//    modifier: Modifier = Modifier
//) {
//    val context = LocalContext.current
//
//    NavHost(
//        navController = navController,
//        startDestination = "all_files",
//        modifier = modifier,
//        // --- 禁用所有转场动画 ---
//        enterTransition = { EnterTransition.None },
//        exitTransition = { ExitTransition.None },
//        popEnterTransition = { EnterTransition.None },
//        popExitTransition = { ExitTransition.None }
//    ) {
//        composable("all_files") {
//            FilesScreen(
//                pdfFiles = viewModel.pdfFiles,
//                onOpenPdf = { pdf -> FileUtils.openPdf(context, pdf) },
//                onRefresh = { viewModel.scanPdfs(context) }
//            )
//        }
//        composable("recent") { /* 最近页面 */ }
//        composable("bookmark") { /* 书签页面 */ }
//        composable("settings") { SettingsScreen() }
//    }
//}