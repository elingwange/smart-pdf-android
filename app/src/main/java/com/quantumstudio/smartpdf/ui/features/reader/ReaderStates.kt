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