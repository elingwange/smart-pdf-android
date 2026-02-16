package com.quantumstudio.smartpdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModelProvider
import com.quantumstudio.smartpdf.data.local.PdfDatabase
import com.quantumstudio.smartpdf.data.repository.PdfRepository
import com.quantumstudio.smartpdf.data.repository.ThemeRepository
import com.quantumstudio.smartpdf.data.scanner.PdfScanner
import com.quantumstudio.smartpdf.ui.features.main.MainScreen
import com.quantumstudio.smartpdf.ui.features.main.MainViewModel
import com.quantumstudio.smartpdf.ui.theme.SmartPDFTheme

// MainActivity.kt

class MainActivity : ComponentActivity() {

    // 不要使用简单的 val viewModel: MainViewModel by viewModels()
    // 而是这样手动初始化：
    private lateinit var viewModel: MainViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // 1. 初始化数据库和 Scanner
        val database = PdfDatabase.getDatabase(this) // 假设你有一个 Room 数据库类
        val scanner = PdfScanner

        // 2. 初始化 Repository
        val pdfRepository = PdfRepository(database.pdfFileDao())
        // 简单实现方案（实际项目中建议使用 Hilt 或 Koin 注入）
        val themeRepository = ThemeRepository(applicationContext)


        // 3. 使用 Factory 创建 ViewModel
        viewModel =
            ViewModelProvider(
                this,
                MainViewModel.Factory(pdfRepository, themeRepository)
            )[MainViewModel::class.java]


        setContent {
            val themeMode by viewModel.themeMode.collectAsState()
            // 1. 使用你定义好的主题包住整个内容
            SmartPDFTheme(themeMode = themeMode) {
                // 2. 这里的 Surface 会自动根据 darkTheme 获取背景色
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    MainScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // 这里也会用到 viewModel，确保它已经初始化
        viewModel.checkPermission(this)
    }
}