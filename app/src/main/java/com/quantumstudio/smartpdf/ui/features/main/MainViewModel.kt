package com.quantumstudio.smartpdf.ui.features.main

import android.content.Context
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.data.model.SortField
import com.quantumstudio.smartpdf.data.model.SortOrder
import com.quantumstudio.smartpdf.data.repository.PdfRepository
import com.quantumstudio.smartpdf.data.repository.ThemeRepository
import com.quantumstudio.smartpdf.ui.common.RefreshPermissionObserver
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@HiltViewModel
class MainViewModel @Inject constructor(
    private val pdfRepository: PdfRepository,
    private val themeRepository: ThemeRepository
) : ViewModel() {

    // 是否拥有权限的状态
    var hasFileAccess by mutableStateOf(false)
        private set
    private val _pdfFiles = MutableStateFlow<List<PdfFile>>(emptyList())
    val pdfFiles = _pdfFiles.asStateFlow()

    fun scanPdfs(context: Context) {
        if (!hasFileAccess) return
        viewModelScope.launch {
            Log.e("---xxxxxxxx", "scanning begin...")
            val scanned = pdfRepository.getAllPdfs(context)
            Log.e("---xxxxxxxx", "scanning end!")

            // 直接发出新的 List 即可触发 UI 更新
            _pdfFiles.value = scanned
        }
    }

    fun createPermissionObserver(checkPermission: () -> Boolean): DefaultLifecycleObserver {
        return RefreshPermissionObserver(
            checkPermission = checkPermission,
            onPermissionGranted = {
                hasFileAccess = true
            }
        )
    }


    @OptIn(ExperimentalCoroutinesApi::class)
    val pdfFilesFlow = snapshotFlow { hasFileAccess } // 1. 将 Compose 状态转为 Flow
        .flatMapLatest { hasAccess ->
            if (hasAccess) {
                // 2. 只有当权限为 true 时，才接入数据库的长连接流
                pdfRepository.getAllPdfsFlow()
            } else {
                // 3. 没权限时，直接发射空列表，断开数据库连接
                flowOf(emptyList())
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // 2. 搜索词状态
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // 3. 核心：通过 combine 实时计算搜索结果
    val searchResult = _searchQuery
        .debounce(300) // 防抖，防止输入太快卡顿
        .combine(pdfFilesFlow) { query, allFiles ->
            if (query.isBlank()) {
                emptyList() // 没输入时不显示结果
            } else {
                allFiles.filter { it.name.contains(query, ignoreCase = true) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }
    //=============== searching ===============


    // 使用 stateIn 将 DataStore 的 Flow 转换为 UI 可用的 StateFlow
    // 初始值设为 SYSTEM
    val themeMode = themeRepository.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    // 存储当前正在阅读的文件状态（单点精准观察）
    var currentReadingPdf by mutableStateOf<PdfFile?>(null)
        private set

    fun loadPdfForReader(path: String) {
        viewModelScope.launch {
            // 1. 强制去数据库里查（或补录）
            val pdf = pdfRepository.getOrInsertPdf(path)
            // 2. 更新这个单点状态
            currentReadingPdf = pdf
            android.util.Log.d("PDF_TRACE", "单点加载完成：${pdf?.name}, 进度: ${pdf?.currentPage}")
        }
    }

    fun updateProgress(path: String, page: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            // 这里的顺序很重要：先确保有记录，再更新进度
            pdfRepository.getOrInsertPdf(path)
            pdfRepository.updateProgress(path, page)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeRepository.saveThemeMode(mode)
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
            pdfRepository.toggleFavorite(path, newStatus)
        }
    }

    fun markAsRead(path: String) {
        viewModelScope.launch {
            // 1. 持久化到数据库
            pdfRepository.markAsRead(path)

            // 2. 更新内存状态，确保 UI 刷新
            _pdfFiles.value = _pdfFiles.value.map { pdf ->
                if (pdf.path == path) pdf.copy(lastReadTime = System.currentTimeMillis()) else pdf
            }
        }
    }

    fun deleteFile(pdf: PdfFile, context: android.content.Context) {
        viewModelScope.launch {
            val success = pdfRepository.deletePdfFile(pdf)
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
            val updatedPdf = pdfRepository.renamePdfFile(pdf, newName)
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


    //=============== sort ===============
    // 排序状态
    private val _sortField = MutableStateFlow(SortField.DATE)
    val sortField = _sortField.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DESCENDING)
    val sortOrder = _sortOrder.asStateFlow()

    // 结合原始文件列表和排序状态
    val sortedPdfFiles = combine(pdfFiles, _sortField, _sortOrder) { files, field, order ->
        when (field) {
            SortField.DATE -> if (order == SortOrder.ASCENDING) files.sortedBy { it.lastModified } else files.sortedByDescending { it.lastModified }
            SortField.NAME -> if (order == SortOrder.ASCENDING) files.sortedBy { it.name.lowercase() } else files.sortedByDescending { it.name.lowercase() }
            SortField.SIZE -> if (order == SortOrder.ASCENDING) files.sortedBy { it.size } else files.sortedByDescending { it.size }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun updateSortConfig(field: SortField, order: SortOrder) {
        _sortField.value = field
        _sortOrder.value = order
    }
}