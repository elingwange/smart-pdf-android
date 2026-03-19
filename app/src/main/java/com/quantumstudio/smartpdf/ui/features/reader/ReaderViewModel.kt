package com.quantumstudio.smartpdf.ui.features.reader

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
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ReaderViewModel @Inject constructor(
    private val pdfRepository: PdfRepository,
    private val pdfActions: PdfActions
) : ViewModel() {

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
            pdfActions.toggleFavorite(pdf)
        }
    }

}