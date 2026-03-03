package com.quantumstudio.smartpdf

import android.animation.ObjectAnimator
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.view.View
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quantumstudio.smartpdf.data.local.PdfDatabase
import com.quantumstudio.smartpdf.data.repository.PdfRepository
import com.quantumstudio.smartpdf.data.repository.ThemeRepository
import com.quantumstudio.smartpdf.ui.features.main.MainScreen
import com.quantumstudio.smartpdf.ui.features.main.MainViewModel
import com.quantumstudio.smartpdf.ui.features.viewer.PdfReaderScreen
import com.quantumstudio.smartpdf.ui.theme.SmartPDFTheme

class MainActivity : ComponentActivity() {

    private lateinit var viewModel: MainViewModel
    private var navController: NavHostController? = null // ✨ 定义导航控制器

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // 1. 初始化 ViewModel
        val database = PdfDatabase.getDatabase(this)
        val pdfRepository = PdfRepository(database.pdfFileDao())
        val themeRepository = ThemeRepository(applicationContext)
        viewModel = ViewModelProvider(
            this,
            MainViewModel.Factory(pdfRepository, themeRepository)
        )[MainViewModel::class.java]

        // 2. 启动页退出动画
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            val slideUp = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.TRANSLATION_Y,
                0f,
                -splashScreenView.view.height.toFloat()
            )
            slideUp.interpolator = FastOutSlowInInterpolator()
            slideUp.duration = 400L
            slideUp.doOnEnd {
                splashScreenView.remove()
                // ✨ 启动页消失后，检查是否有快捷方式进来的 Intent
                handleIntent(intent)
            }
            slideUp.start()
        }

        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            val currentNavController = rememberNavController()
            navController = currentNavController // ✨ 赋值给成员变量供 handleIntent 使用

            SmartPDFTheme(themeMode = themeMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // --- ✨ 导航核心配置 ---
                    NavHost(
                        navController = currentNavController,
                        startDestination = "main" // 默认进入首页
                    ) {
                        // 首页
                        composable("main") {
                            MainScreen(
                                viewModel = viewModel,
                                onNavigateToReader = { uri ->
                                    val encodedUri = Uri.encode(uri.toString())
                                    currentNavController.navigate("reader/$encodedUri")
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
                                viewModel = viewModel,
                                onBack = { currentNavController.popBackStack() }
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent) // 关键：更新 Intent，否则 handleIntent 拿到的还是旧数据
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        intent?.data?.let { uri ->
            val encodedUri = Uri.encode(uri.toString())
            // ✨ 使用生命周期安全的导航调用
            navController?.navigate("reader/$encodedUri") {
                // 如果已经在阅读器里了，弹出当前页面重新进入，避免堆栈重复
                launchSingleTop = true
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkPermission(this)
    }
}