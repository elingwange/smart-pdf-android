package com.quantumstudio.smartpdf.ui.viewmodel

import android.app.Application
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quantumstudio.smartpdf.model.PdfFile
import com.quantumstudio.smartpdf.util.PdfScanner
import kotlinx.coroutines.launch

class HomeViewModel(application: Application) : AndroidViewModel(application) {
    private val scanner = PdfScanner(application)

    // UI 观察这个状态
    var pdfFiles by mutableStateOf<List<PdfFile>>(emptyList())
        private set

    var isLoading by mutableStateOf(false)
        private set

    // 在 ViewModel 的 loadFiles 顶部添加
    fun scanManual(context: Context) {
        val path = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString()
        MediaScannerConnection.scanFile(context, arrayOf(path), null) { _, _ ->
            // 扫描完成后再执行你的查询逻辑
            loadFiles()
        }
    }
    fun loadFiles() {
        viewModelScope.launch {
            isLoading = true
            pdfFiles = scanner.scanPdfs()
            isLoading = false
        }
    }
}