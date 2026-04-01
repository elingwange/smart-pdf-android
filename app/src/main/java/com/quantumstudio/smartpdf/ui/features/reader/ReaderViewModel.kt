package com.quantumstudio.smartpdf.ui.features.reader

import android.app.Application
import android.net.Uri
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.data.repository.PdfRepository
import com.quantumstudio.smartpdf.ui.common.PdfActions
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File
import javax.inject.Inject


@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val application: Application, // 使用 Application Context 避免内存泄漏
    private val pdfRepository: PdfRepository,
    private val pdfActions: PdfActions
) : ViewModel() {
    // 内部维护 PDF 加载的状态流
    private val _loadStatus = MutableStateFlow<PdfLoadStatus>(PdfLoadStatus.Idle)
    val loadStatus = _loadStatus.asStateFlow()

    fun loadPdf(source: String) {
        Log.d("--ELog", "loadPdf called with source: $source")

        viewModelScope.launch(Dispatchers.IO) {
            _loadStatus.value = PdfLoadStatus.Loading

            try {
                val uri = Uri.parse(source)
                val scheme = uri.scheme

                // --- 策略 A：如果是本地物理路径且可读，直接起飞 ---
                val potentialFile = if (scheme == "file") File(uri.path ?: "") else File(source)
                if (potentialFile.exists() && potentialFile.canRead()) {
                    _loadStatus.value = PdfLoadStatus.Success(potentialFile)
                    return@launch
                }

                // --- 策略 B：如果是 Content URI 或 权限受限的 File，执行急救拷贝 ---
                if (scheme == "content" || scheme == "file" || source.startsWith("/")) {
                    val fileName = "incoming_${System.currentTimeMillis()}.pdf"
                    val tempFile = File(application.cacheDir, fileName)

                    application.contentResolver.openInputStream(uri)?.use { input ->
                        tempFile.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                    if (tempFile.exists() && tempFile.length() > 0) {
                        clearOldCacheExcept(fileName)
                        _loadStatus.value = PdfLoadStatus.Success(tempFile)
                    } else {
                        _loadStatus.value = PdfLoadStatus.Error("无法保存临时文件，空间可能不足")
                    }
                } else {
                    _loadStatus.value = PdfLoadStatus.Error("无效的文件路径格式")
                }
            } catch (e: Exception) {
                Log.e("--ELog", "Load Error", e)
                _loadStatus.value = PdfLoadStatus.Error("文件访问失败: ${e.localizedMessage}")
            }
        }
    }

    private fun clearOldCacheExcept(currentName: String) {
        application.cacheDir.listFiles { _, name ->
            name.startsWith("incoming_") && name != currentName
        }?.forEach { it.delete() }
    }

    // 存储当前正在阅读的文件状态
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

    fun markAsRead(pdf: PdfFile) {
        viewModelScope.launch {
            pdfActions.markAsRead(pdf)
        }
    }

    fun toggleFavorite(pdf: PdfFile) {
        viewModelScope.launch {
            // 1. 执行数据库更新操作
            pdfActions.toggleFavorite(pdf)

            // 2. ✨ 关键：重新查一次数据库，或者手动创建一个 copy 赋给 currentReadingPdf
            // 只有 currentReadingPdf 指向了一个新的对象地址，Compose 才会感知到变化
            val updatedPdf = pdfRepository.getOrInsertPdf(pdf.path)
            currentReadingPdf = updatedPdf
        }
    }

}