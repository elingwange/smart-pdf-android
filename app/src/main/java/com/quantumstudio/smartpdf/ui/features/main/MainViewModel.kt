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
import kotlinx.coroutines.flow.distinctUntilChanged
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

    private var isScanning = false // 简单的状态锁

    fun scanPdfs(context: Context) {
        // 1. 权限检查 + 状态检查
        if (!hasFileAccess || isScanning) return

        viewModelScope.launch(Dispatchers.IO) {
            try {
                isScanning = true
                Log.d("SmartPDF", "开始全盘扫描...")

                // 2. 调用 Repository 执行真正的 IO 操作（写数据库）
                // 只要 getAllPdfs 内部执行了 roomDao.insert()，UI 就会自动刷新
                pdfRepository.getAllPdfs(context)

                Log.d("SmartPDF", "扫描完成，数据库已更新")
            } catch (e: Exception) {
                Log.e("SmartPDF", "扫描出错", e)
            } finally {
                isScanning = false
            }
        }
    }

    // ✨ 核心优化 1：数据源唯一化。直接从数据库拿流，只要库里变，这里秒变。
    @OptIn(ExperimentalCoroutinesApi::class)
    val allPdfsFlow = snapshotFlow { hasFileAccess }
        .distinctUntilChanged() // ✨ 只在权限真正发生切换时才重连数据库
        .flatMapLatest { granted ->
            if (granted) pdfRepository.getAllPdfsFlow() else flowOf(emptyList())
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())


    fun createPermissionObserver(checkPermission: () -> Boolean): DefaultLifecycleObserver {
        return RefreshPermissionObserver(
            checkPermission = checkPermission,
            onPermissionGranted = {
                hasFileAccess = true
            }
        )
    }

    // 2. 搜索词状态
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // 3. 核心：通过 combine 实时计算搜索结果
    val searchResult = _searchQuery
        .debounce(300) // 防抖，防止输入太快卡顿
        .combine(allPdfsFlow) { query, allFiles ->
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

    // ✨ 核心优化 2：操作命令化。只负责改库，不负责改内存列表。
    fun toggleFavorite(pdf: PdfFile) {
        viewModelScope.launch {
            // 这一行执行完，Room 数据库会自动通知 allPdfsFlow 发射新数据
            pdfRepository.toggleFavorite(pdf.path, !pdf.isFavorite)
        }
    }

    fun markAsRead(pdf: PdfFile) {
        viewModelScope.launch {
            // 1. 持久化到数据库
            pdfRepository.markAsRead(pdf.path)
        }
    }

    // TODO: 修改提示方式，不需要context
    fun deleteFile(pdf: PdfFile, context: Context) {
        viewModelScope.launch {
            val success = pdfRepository.deletePdfFile(pdf)
            if (success) {
                android.widget.Toast.makeText(
                    context,
                    "File deleted",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            } else {
                android.widget.Toast.makeText(
                    context,
                    "Delete failed",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
            }
        }
    }

    fun renameFile(pdf: PdfFile, newName: String) {
        viewModelScope.launch {
            val updatedPdf = pdfRepository.renamePdfFile(pdf, newName)
        }
    }


    //=============== sort ===============
    // 排序状态
    private val _sortField = MutableStateFlow(SortField.DATE)
    val sortField = _sortField.asStateFlow()

    private val _sortOrder = MutableStateFlow(SortOrder.DESCENDING)
    val sortOrder = _sortOrder.asStateFlow()

    // 结合原始文件列表和排序状态
    val sortedPdfFiles = combine(allPdfsFlow, _sortField, _sortOrder) { files, field, order ->
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