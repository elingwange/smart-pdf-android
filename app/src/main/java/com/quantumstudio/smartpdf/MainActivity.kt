package com.quantumstudio.smartpdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.ViewModelProvider
import com.quantumstudio.smartpdf.data.local.PdfDatabase
import com.quantumstudio.smartpdf.data.repository.PdfRepository
import com.quantumstudio.smartpdf.data.scanner.PdfScanner
import com.quantumstudio.smartpdf.ui.features.main.MainScreen
import com.quantumstudio.smartpdf.ui.features.main.MainViewModel

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
        val repository = PdfRepository(database.pdfFileDao())

        // 3. 使用 Factory 创建 ViewModel
        viewModel =
            ViewModelProvider(this, MainViewModel.Factory(repository))[MainViewModel::class.java]

        setContent {
            MainScreen(viewModel = viewModel)
        }
    }

    override fun onResume() {
        super.onResume()
        // 这里也会用到 viewModel，确保它已经初始化
        viewModel.checkPermission(this)
    }
}