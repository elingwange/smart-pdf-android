package com.quantumstudio.smartpdf

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.navigation.NavHostController
import com.quantumstudio.smartpdf.ui.SmartPDFRoot
import com.quantumstudio.smartpdf.ui.features.main.MainViewModel
import com.quantumstudio.smartpdf.ui.features.reader.ReaderViewModel
import com.quantumstudio.smartpdf.ui.features.settings.SettingsViewModel
import com.quantumstudio.smartpdf.ui.navigation.AppNavHost
import com.quantumstudio.smartpdf.util.PermissionManager
import com.quantumstudio.smartpdf.util.installCustomExitAnimation
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    @Inject
    lateinit var permissionManager: PermissionManager
    private val mainViewModel: MainViewModel by viewModels()
    private val readerViewModel: ReaderViewModel by viewModels()

    private val settingsViewModel: SettingsViewModel by viewModels()
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
                settingsViewModel = settingsViewModel,
                onCreated = { navController = it }
            ) { controller ->
                AppNavHost(controller, mainViewModel, readerViewModel)
            }
        }
    }

    fun initObservers() {
        lifecycle.addObserver(mainViewModel.createPermissionObserver {
            permissionManager.isStoragePermissionGranted()
        })
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
