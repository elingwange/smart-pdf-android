package com.quantumstudio.smartpdf.ui.features.reader

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
import java.io.File

enum class ReaderPanel { None, Brightness, Jump }

sealed class PdfLoadStatus {
    object Idle : PdfLoadStatus()
    object Loading : PdfLoadStatus()
    data class Success(val file: File) : PdfLoadStatus()
    data class Error(val message: String) : PdfLoadStatus()
}

class ReaderUiState(
    initialIsNightMode: Boolean = false,
    initialIsUiVisible: Boolean = true
) {
    var pdfLoadStatus by mutableStateOf<PdfLoadStatus>(PdfLoadStatus.Idle)

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

    // 亮度范围 0f - 1f
    var currentBrightness by mutableFloatStateOf(0.5f)
        private set

    fun updateBrightness(value: Float) {
        currentBrightness = value
    }

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

    // 修改打开逻辑：在打开亮度面板时，允许外部传入当前系统亮度
    fun toggleBrightness(systemBrightness: Float) {
        if (activePanel != ReaderPanel.Brightness) {
            currentBrightness = if (systemBrightness < 0f) {
                // 如果系统是自动亮度(-1f)，尝试给个 0.5f 或者更接近真实感的默认值
                0.5f
            } else {
                systemBrightness
            }
            activePanel = ReaderPanel.Brightness
        } else {
            activePanel = ReaderPanel.None
        }
    }

    fun resetToSystemBrightness() {
        currentBrightness = -1f // 使用 -1f 作为标志位
    }

    // 检查当前是否处于系统自动模式
    val isAutoBrightness: Boolean
        get() = currentBrightness < 0f

    fun setSystemAutoBrightness() {
        currentBrightness = -1f // 设为 -1f 标志位
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
            // 1. 保存时增加 currentBrightness (下标为 3)
            save = {
                listOf(
                    it.isUiVisible,
                    it.isNightMode,
                    it.activePanel.name,
                    it.currentBrightness
                )
            },
            restore = { savedList ->
                val list = savedList as List<*>
                ReaderUiState(
                    initialIsUiVisible = list[0] as Boolean,
                    initialIsNightMode = list[1] as Boolean
                ).apply {
                    val panelName = list[2] as String
                    activePanel = ReaderPanel.valueOf(panelName)

                    // 2. 恢复亮度值
                    val savedBrightness = list[3] as Float
                    updateBrightness(savedBrightness)
                }
            }
        )
    }
}

class PdfViewState {
    var currentPage by mutableIntStateOf(0)
    var totalPages by mutableIntStateOf(0)
    var scrollProgress by mutableFloatStateOf(0f)
    var isFirstLoad by mutableStateOf(true)

    // ✨ 关键修复：改用 Path 字符串，绕过 Uri 序列化复杂的坑
    var lastLoadedFilePath by mutableStateOf<String?>(null)

    var scrollSignal by mutableLongStateOf(System.currentTimeMillis())
    var pdfView by mutableStateOf<PDFView?>(null)

    fun updatePage(page: Int, count: Int) {
        currentPage = page
        totalPages = count
    }

    fun updateScroll(page: Int, offset: Float) {
        // 防止除以 0 导致的 NaN
        if (totalPages > 1) {
            scrollProgress = (page + offset) / (totalPages - 1).toFloat()
        } else {
            scrollProgress = 0f
        }
        scrollSignal = System.currentTimeMillis()
    }

    companion object {
        val Saver: Saver<PdfViewState, *> = Saver(
            save = { listOf(it.currentPage, it.lastLoadedFilePath) },
            restore = { saved ->
                val list = saved as List<*>
                PdfViewState().apply {
                    currentPage = list[0] as Int
                    lastLoadedFilePath = list[1] as? String
                }
            }
        )
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