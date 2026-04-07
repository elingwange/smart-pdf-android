package com.quantumstudio.smartpdf.ui.features.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.data.repository.PdfRepository2
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel2 @Inject constructor(
    private val repo: PdfRepository2
) : ViewModel() {

    var hasFileAccess by mutableStateOf(true)
        private set

    // 1. 定义状态源
    val searchQuery = MutableStateFlow("")
    val sortField = MutableStateFlow("lastModified") // 对应数据库字段名
    val isAsc = MutableStateFlow(false)

    // 2. 构造响应式分页流
    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    val pdfFlow: Flow<PagingData<PdfFile>> = combine(
        searchQuery.debounce(300), // 🔍 低端机必备：等用户停笔 300ms 再查数据库
        sortField,
        isAsc
    ) { query, field, asc ->
        // 这里返回的是一个 Flow<PagingData<PdfFile>>
        repo.getPagingData(query, field, asc)
    }.flatMapLatest { pagingFlow ->
        // 拍平嵌套流：Flow<Flow<PagingData>> -> Flow<PagingData>
        pagingFlow
    }.cachedIn(viewModelScope) // 💾 将分页数据缓存在 ViewModel 作用域，防止旋转屏幕重启加载

    init {
        viewModelScope.launch(Dispatchers.IO) {
            // 建议 1：给 UI 渲染留出一点点“首屏时间”
            delay(500)

            // 建议 2：使用更保守的并发限制（仅针对极低端机）
            // 确保不会因为扫描太猛把 IO 带宽占满
            repo.syncDiskToDb()
        }
    }

    // 4. 提供给 UI 的交互方法
    fun onSearch(query: String) {
        searchQuery.value = query
    }

    fun onSortChange(field: String, ascending: Boolean) {
        sortField.value = field
        isAsc.value = ascending
    }
}