package com.quantumstudio.smartpdf.ui.common


sealed class UiEvent {
    data class ShowSnackBar(
        val message: String,
        val actionLabel: String? = null,
        val onAction: (() -> Unit)? = null // 点击“撤销”时的回调
    ) : UiEvent()
}