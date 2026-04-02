package com.quantumstudio.smartpdf.ui.features.main

import android.app.Application
import android.content.Context
import android.net.Uri
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
import com.quantumstudio.smartpdf.ui.common.PdfActions
import com.quantumstudio.smartpdf.ui.common.RefreshPermissionObserver
import com.quantumstudio.smartpdf.ui.common.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val pdfRepository: PdfRepository,
    private val pdfActions: PdfActions,
    application: Application
) : ViewModel() {

    private val _pendingReaderUri = MutableStateFlow<Uri?>(null)
    val pendingReaderUri = _pendingReaderUri.asStateFlow()

    fun consumePendingUri() {
        _pendingReaderUri.value = null
    }

    var hasFileAccess by mutableStateOf(false)
        private set

    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    //=============== sort ===============
    private val _sortField = MutableStateFlow(SortField.DATE)
    val sortField = _sortField.asStateFlow()
    private val _sortOrder = MutableStateFlow(SortOrder.DESCENDING)
    val sortOrder = _sortOrder.asStateFlow()

    //-----------------------------------------------------------

    // 维度 4：生命周期。所有的异步逻辑都绑定在 viewModelScope
    private val _isScanning = MutableStateFlow(false)

    fun scanPdfs(context: Context) {
        if (!hasFileAccess || _isScanning.value) return

        viewModelScope.launch {
            try {
                _isScanning.value = true
                // 调用封装好的同步逻辑
                pdfRepository.syncPdfFiles(context)
            } catch (e: Exception) {
            } finally {
                _isScanning.value = false
            }
        }
    }

    // 优化后的排序逻辑：增加 debounce 机制
    @OptIn(FlowPreview::class)
    val sortedPdfFiles = combine(
        pdfRepository.getAllPdfsFlow(application), // 直接观察数据库流
        _sortField,
        _sortOrder,
        _searchQuery.debounce(300) // 搜索防抖，避免 TCL 603 高频重组 UI
    ) { files, field, order, query ->
        // 1. 先过滤
        val filtered = if (query.isBlank()) files
        else files.filter { it.name.contains(query, ignoreCase = true) }

        // 2. 再排序
        when (field) {
            SortField.DATE -> if (order == SortOrder.ASCENDING) filtered.sortedBy { it.lastModified } else filtered.sortedByDescending { it.lastModified }
            SortField.NAME -> if (order == SortOrder.ASCENDING) filtered.sortedBy { it.name.lowercase() } else filtered.sortedByDescending { it.name.lowercase() }
            SortField.SIZE -> if (order == SortOrder.ASCENDING) filtered.sortedBy { it.size } else filtered.sortedByDescending { it.size }
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(ExperimentalCoroutinesApi::class)
    val allPdfsFlow = snapshotFlow { hasFileAccess }
        .distinctUntilChanged()
        .flatMapLatest { granted ->
            if (granted) pdfRepository.getAllPdfsFlow(application)
            else flowOf(emptyList())
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    @OptIn(FlowPreview::class)
    val searchResult = _searchQuery
        .debounce(300)
        .combine(allPdfsFlow) { query, allFiles ->
            if (query.isBlank()) {
                emptyList()
            } else {
                allFiles.filter { it.name.contains(query, ignoreCase = true) }
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 利用 Flow 组合器，在后台线程完成过滤
    val favoriteFiles = sortedPdfFiles.map { list ->
        list.filter { it.isFavorite }
    }.flowOn(Dispatchers.Default) // 👈 确保计算不占用主线程
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    // 在后台线程准备好成品
    val recentFiles = allPdfsFlow
        .map { list ->
            list.filter { it.lastReadTime > 0 }
                .sortedByDescending { it.lastReadTime }
        }
        .flowOn(Dispatchers.Default) // 👈 关键：切换到计算线程
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    fun createPermissionObserver(checkPermission: () -> Boolean): DefaultLifecycleObserver {
        return RefreshPermissionObserver(
            checkPermission = checkPermission,
            onPermissionGranted = {
                hasFileAccess = true
            }
        )
    }

    fun onQueryChange(newQuery: String) {
        _searchQuery.value = newQuery
    }

    fun toggleFavorite(pdf: PdfFile) {
        viewModelScope.launch {
            pdfActions.toggleFavorite(pdf)
        }
    }

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun deleteFile(pdf: PdfFile) {
        viewModelScope.launch {
            _uiEvent.send(
                UiEvent.ShowSnackBar(
                    message = "已删除 ${pdf.name}",
                    onAction = {
                    }
                )
            )
            val success = pdfRepository.deletePdfFile(pdf)
        }
    }

    fun renameFile(pdf: PdfFile, newName: String) {
        viewModelScope.launch {
            val updated = pdfRepository.renamePdfFile(pdf, newName)
            if (updated != null) {
                //               _uiEvent.send(UiEvent.ShowToast("Renamed successfully"))
            }
        }
    }

    fun updateSortConfig(field: SortField, order: SortOrder) {
        _sortField.value = field
        _sortOrder.value = order
    }
}
