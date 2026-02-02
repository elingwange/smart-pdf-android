package com.quantumstudio.smartpdf.ui.viewmodel

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantumstudio.smartpdf.pdf.data.PdfFile
import com.quantumstudio.smartpdf.pdf.data.PdfScanner
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainViewModel : ViewModel() {
    // 使用 Compose 状态
    private val _pdfFiles = mutableStateListOf<PdfFile>()
    val pdfFiles: List<PdfFile> = _pdfFiles

    // 是否拥有权限的状态
    var hasFileAccess by mutableStateOf(false)
        private set

    // 检查权限
    fun checkPermission(context: Context) {
        hasFileAccess = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun scanPdfs(context: Context) {
        viewModelScope.launch {
            val scanned = withContext(Dispatchers.IO) {
                PdfScanner.scanAllPdfFiles(context)
            }
            _pdfFiles.clear()
            _pdfFiles.addAll(scanned)
        }
    }
}