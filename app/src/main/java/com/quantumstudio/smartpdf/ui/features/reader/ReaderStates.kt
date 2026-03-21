package com.quantumstudio.smartpdf.ui.features.reader

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.github.barteksc.pdfviewer.PDFView

enum class ReaderPanel { None, Brightness, Jump }

class ReaderUiState(
    initialIsNightMode: Boolean = false,
    initialIsUiVisible: Boolean = true
) {
    var isUiVisible by mutableStateOf(initialIsUiVisible)
        private set

    // 这里的 setter 是私有的，编译器生成的也是 private setNightMode
    var isNightMode by mutableStateOf(initialIsNightMode)
        private set

    var activePanel by mutableStateOf(ReaderPanel.None)
        private set

    var showInfoDialog by mutableStateOf(false)
    var isPageIndicatorVisible by mutableStateOf(false)

    // --- 逻辑控制方法 ---

    fun toggleUi() {
        isUiVisible = !isUiVisible
        if (!isUiVisible) activePanel = ReaderPanel.None
    }

    // 方案 A：直接使用 toggle 逻辑
    fun toggleNightMode() {
        isNightMode = !isNightMode
    }

    // 方案 B：如果确实需要手动设置特定值，改名为 applyNightMode 或 updateNightMode
    // 避开 setNightMode 这个名字
    fun applyNightMode(enabled: Boolean) {
        isNightMode = enabled
    }

    fun toggleBrightness() {
        activePanel =
            if (activePanel == ReaderPanel.Brightness) ReaderPanel.None else ReaderPanel.Brightness
    }

    fun toggleJump() {
        activePanel = if (activePanel == ReaderPanel.Jump) ReaderPanel.None else ReaderPanel.Jump
    }

    fun closePanels() {
        activePanel = ReaderPanel.None
    }

    // ✨ 核心：定义如何保存和恢复状态
    companion object {
        val Saver: Saver<ReaderUiState, *> = Saver(
            // save: 将类属性拆解为 List 存入 Bundle
            save = { listOf(it.isUiVisible, it.isNightMode, it.activePanel.name) },
            // restore: 从 List 中读取数据并重建 ReaderUiState 对象
            restore = { savedList ->
                val list = savedList as List<*>
                ReaderUiState(
                    initialIsUiVisible = list[0] as Boolean,
                    initialIsNightMode = list[1] as Boolean
                ).apply {
                    // 恢复面板状态
                    val panelName = list[2] as String
                    if (panelName != ReaderPanel.None.name) {
                        // 根据保存的名字恢复枚举值
                        when (panelName) {
                            ReaderPanel.Brightness.name -> toggleBrightness()
                            ReaderPanel.Jump.name -> toggleJump()
                        }
                    }
                }
            }
        )
    }
}

// --- PDF 运行大管家 ---
class PdfViewState {
    var currentPage by mutableIntStateOf(0)
    var totalPages by mutableIntStateOf(0)
    var scrollProgress by mutableFloatStateOf(0f)
    var isFirstLoad by mutableStateOf(true)
    var lastLoadedUri by mutableStateOf<Uri?>(null)
    var scrollSignal by mutableLongStateOf(System.currentTimeMillis())

    // 引用 PDFView 实例用于跳转等操作
    var pdfView by mutableStateOf<PDFView?>(null)

    fun updatePage(page: Int, count: Int) {
        currentPage = page
        totalPages = count
    }

    fun updateScroll(page: Int, offset: Float) {
        if (totalPages > 1) {
            scrollProgress = (page + offset) / (totalPages - 1).toFloat()
        }
        scrollSignal = System.currentTimeMillis()
    }
}

@Composable
fun rememberReaderUiState(initialIsNightMode: Boolean = false) =
    rememberSaveable(saver = ReaderUiState.Saver) {
        ReaderUiState(initialIsNightMode = initialIsNightMode)
    }

@Composable
fun rememberPdfViewState() = rememberSaveable(
    saver = Saver(
        save = { it.currentPage },
        restore = { savedPage -> PdfViewState().apply { currentPage = savedPage as Int } }
    )
) {
    PdfViewState()
}