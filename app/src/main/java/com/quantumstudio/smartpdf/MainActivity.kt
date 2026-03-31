package com.quantumstudio.smartpdf

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavHostController
import com.quantumstudio.smartpdf.ui.SmartPDFRoot
import com.quantumstudio.smartpdf.ui.features.main.MainViewModel
import com.quantumstudio.smartpdf.ui.features.reader.ReaderViewModel
import com.quantumstudio.smartpdf.ui.features.settings.SettingsViewModel
import com.quantumstudio.smartpdf.ui.navigation.AppNavHost
import com.quantumstudio.smartpdf.util.PermissionManager
import com.quantumstudio.smartpdf.util.installCustomExitAnimation
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
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
        // 1. 初始化 Splash Screen
        val splashScreen = installSplashScreen()
        splashScreen.installCustomExitAnimation {
            // 这里通常不直接处理 Intent，因为 NavController 还没初始化
        }

        super.onCreate(savedInstanceState)

        initObservers()

        handleIntent(intent)

        setContent {
            SmartPDFRoot(
                settingsViewModel = settingsViewModel,
                mainViewModel = mainViewModel,
                readerViewModel = readerViewModel,
                onCreated = {
                    navController = it
                    // ✨ 关键：只有当 NavController 准备好后，才处理 Intent
                    handleIntent(intent)
                }
            ) { controller ->
                AppNavHost(controller, mainViewModel, readerViewModel)
            }
        }
    }

    private fun initObservers() {
        lifecycle.addObserver(mainViewModel.createPermissionObserver {
            permissionManager.isStoragePermissionGranted()
        })
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        val uri = intent?.data ?: return

        // 关键点：这是 Activity 还在前台，权限最稳的时候
        if (uri.scheme == "content") {
            // 直接在 Activity 层面启动一个简单的拷贝任务
            // 或者调用 ViewModel 的方法，但必须确保传入的是当前 Activity 授权的 URI
            lifecycleScope.launch(Dispatchers.IO) {
                val localFile = saveUriToCache(uri)
                if (localFile != null) {
                    withContext(Dispatchers.Main) {
                        // 跳转时，不再传复杂的 content:// URI，直接传本地缓存路径
                        val encodedPath = Uri.encode(localFile.absolutePath)
                        navController?.navigate("reader/$encodedPath")
                    }
                }
            }
        }
    }

    private fun saveUriToCache(uri: Uri): File? {
        return try {
            val fileName = "external_${System.currentTimeMillis()}.pdf"
            val tempFile = File(cacheDir, fileName)
            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output -> input.copyTo(output) }
            }
            if (tempFile.exists() && tempFile.length() > 0) tempFile else null
        } catch (e: Exception) {
            Log.e("--ELog", "Pre-cache failed: ${e.message}")
            null
        }
    }
}