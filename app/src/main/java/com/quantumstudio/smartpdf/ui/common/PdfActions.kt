package com.quantumstudio.smartpdf.ui.common

import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.data.repository.PdfRepository
import javax.inject.Inject

class PdfActions @Inject constructor(
    private val pdfRepository: PdfRepository
) {
    // 定义为 suspend，把线程权交给调用者
    suspend fun toggleFavorite(pdf: PdfFile) {
        pdfRepository.toggleFavorite(pdf.path, !pdf.isFavorite)
    }

    suspend fun markAsRead(pdf: PdfFile) {
        pdfRepository.markAsRead(pdf.path)
    }
}