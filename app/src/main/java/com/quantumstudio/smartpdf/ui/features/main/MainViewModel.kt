package com.quantumstudio.smartpdf.ui.features.main

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.data.repository.PdfRepository
import com.quantumstudio.smartpdf.data.repository.ThemeRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

class MainViewModel(
    private val repository: PdfRepository,
    private val themeRepository: ThemeRepository
) : ViewModel() {
    // 使用 stateIn 将 DataStore 的 Flow 转换为 UI 可用的 StateFlow
    // 初始值设为 SYSTEM
    val themeMode = themeRepository.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeRepository.saveThemeMode(mode)
        }
    }

    // 改用 StateFlow
    private val _pdfFiles = MutableStateFlow<List<PdfFile>>(emptyList())
    val pdfFiles = _pdfFiles.asStateFlow()

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
//            _pdfFiles.clear()
//            _pdfFiles.addAll(scanned)
            // 直接发出新的 List 即可触发 UI 更新
            _pdfFiles.value = scanned
        }
    }

    fun toggleFavorite(path: String) {
        viewModelScope.launch {
            // 1. 获取当前列表中的状态
            val currentFiles = _pdfFiles.value
            val fileToUpdate = currentFiles.find { it.path == path } ?: return@launch
            val newStatus = !fileToUpdate.isFavorite

            // 2. 立即更新内存 UI
            _pdfFiles.value = currentFiles.map {
                if (it.path == path) it.copy(isFavorite = newStatus) else it
            }

            // 3. 异步持久化到数据库
            repository.toggleFavorite(path, newStatus)
        }
    }

    fun markAsRead(path: String) {
        viewModelScope.launch {
            // 1. 持久化到数据库
            repository.markAsRead(path)

            // 2. 更新内存状态，确保 UI 刷新
            _pdfFiles.value = _pdfFiles.value.map { pdf ->
                if (pdf.path == path) pdf.copy(lastReadTime = System.currentTimeMillis()) else pdf
            }
        }
    }

    fun deleteFile(pdf: PdfFile, context: android.content.Context) {
        viewModelScope.launch {
            val success = repository.deletePdfFile(pdf)
            if (success) {
                // 1. 更新内存中的列表，让 UI 立即刷新
                _pdfFiles.value = _pdfFiles.value.filter { it.path != pdf.path }

                // 2. 提示用户
                android.widget.Toast.makeText(
                    context,
                    "File deleted",
                    android.widget.Toast.LENGTH_SHORT
                ).show()

                // 3. 重要：通知系统扫描媒体库，防止其他 App 还以为这文件存在
                android.media.MediaScannerConnection.scanFile(
                    context, arrayOf(pdf.path), null, null
                )
            } else {
                android.widget.Toast.makeText(
                    context,
                    "Delete failed",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun renameFile(pdf: PdfFile, newName: String, context: android.content.Context) {
        viewModelScope.launch {
            val updatedPdf = repository.renamePdfFile(pdf, newName)
            if (updatedPdf != null) {
                // 更新 UI 列表：替换掉旧对象
                _pdfFiles.value = _pdfFiles.value.map {
                    if (it.path == pdf.path) updatedPdf else it
                }
                // 通知系统媒体库更新
                android.media.MediaScannerConnection.scanFile(
                    context, arrayOf(pdf.path, updatedPdf.path), null, null
                )
            } else {
                android.widget.Toast.makeText(
                    context,
                    "Rename failed",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    // 添加工厂类
    class Factory(
        private val pdfRepository: PdfRepository,
        private val themeRepository: ThemeRepository
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
                @Suppress("UNCHECKED_CAST")
                return MainViewModel(pdfRepository, themeRepository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}