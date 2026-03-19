package com.quantumstudio.smartpdf

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import com.quantumstudio.smartpdf.ui.SmartPDFRoot
import com.quantumstudio.smartpdf.ui.features.main.MainViewModel
import com.quantumstudio.smartpdf.ui.navigation.AppNavHost
import com.quantumstudio.smartpdf.util.PermissionManager
import com.quantumstudio.smartpdf.util.installCustomExitAnimation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var permissionManager: PermissionManager
    private val viewModel: MainViewModel by viewModels()
    private var navController: NavHostController? = null

    @RequiresApi(Build.VERSION_CODES.N_MR1)
    override fun onCreate(savedInstanceState: Bundle?) {

        installSplashScreen().installCustomExitAnimation() {
            handleIntent(intent)
        }

        super.onCreate(savedInstanceState)

        initObservers()

        setContent {
            SmartPDFRoot(
                viewModel = viewModel,
                onCreated = { navController = it } // 直接赋值给成员变量
            ) { controller ->
                AppNavHost(controller, viewModel)
            }
        }
    }

    fun initObservers() {
        lifecycle.addObserver(viewModel.createPermissionObserver {
            permissionManager.isStoragePermissionGranted()
        })

//        lifecycle.addObserver(viewModel.createPermissionObserver(this))
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        // 关键：更新 Intent，否则 handleIntent 拿到的还是旧数据
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return // 快速失败，减少嵌套
        val encodedUri = Uri.encode(uri.toString())
        navController?.navigate("reader/$encodedUri") {
            launchSingleTop = true
            // 如果已经在首页了，不希望点击快捷方式后还能点“返回”回到之前的状态
            // 这种方式能让导航栈更干净
            popUpTo("main") { saveState = true }
        }
        // 处理完后将 data 置空
        // 这样在 Activity 因系统原因（如主题切换）重启时，不会再次触发 handleIntent
        intent.data = null
    }

}
