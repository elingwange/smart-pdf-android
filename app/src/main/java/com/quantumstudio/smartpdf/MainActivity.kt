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
        Log.d("---ELog", "Handling Intent URI: $uri, Scheme: ${uri.scheme}")

        when (uri.scheme) {
            "content" -> {
                // 情况 A：外部应用（Gmail/WeChat）传入的临时权限 URI -> 执行拷贝
                lifecycleScope.launch(Dispatchers.IO) {
                    val localFile = saveUriToCache(uri) // 你刚才写的拷贝逻辑
                    if (localFile != null) {
                        withContext(Dispatchers.Main) {
                            Log.d(
                                "---ELog",
                                "content-> localFile.absolutePath: $localFile.absolutePath"
                            )
                            navigateToReader(localFile.absolutePath)
                        }
                    }
                }
            }

            "file" -> {
                // 情况 B：桌面快捷方式或本地文件浏览器传入的直接路径
                // 注意：file://path 需要去掉协议头，或者直接拿 path
                val path = uri.path ?: return
                Log.d("---ELog", "file-> path: $path")
                navigateToReader(path)
            }

            else -> {
                // 情况 C：某些情况下直接传的是绝对路径字符串
                Log.d("---ELog", "else—> uri.toString(): $uri.toString()")
                navigateToReader(uri.toString())
            }
        }
    }

    private fun navigateToReader(path: String) {
        // 统一编码路径并跳转
        val encodedPath = Uri.encode(path)
        navController?.navigate("reader/$encodedPath") {
            // 建议：如果是从快捷方式进入，清空栈，防止按返回键回到一个空的 MainActivity
            popUpTo(navController!!.graph.startDestinationId) { saveState = true }
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
            Log.e("---ELog", "Pre-cache failed: ${e.message}")
            null
        }
    }
}