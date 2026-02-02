package com.quantumstudio.smartpdf.ui.features.main

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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.data.repository.PdfRepository
import kotlinx.coroutines.launch

class MainViewModel(private val repository: PdfRepository) : ViewModel() {
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
        if (!hasFileAccess) return
        viewModelScope.launch {
            val scanned = repository.getAllPdfs(context)
            _pdfFiles.clear()
            _pdfFiles.addAll(scanned)
        }
    }

    // 添加工厂类
    class Factory(private val repository: PdfRepository) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}