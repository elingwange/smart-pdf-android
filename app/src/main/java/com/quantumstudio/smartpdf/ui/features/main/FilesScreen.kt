package com.quantumstudio.smartpdf.ui.features.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.ui.components.PdfActionSheet
import com.quantumstudio.smartpdf.ui.components.PdfListItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    pdfFiles: List<PdfFile>,
    onOpenPdf: (PdfFile) -> Unit,
    onRefresh: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState()
    var selectedPdf by remember { mutableStateOf<PdfFile?>(null) }
    var showSheet by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFC0C0C0))
            .padding(16.dp)
    ) {
        Button(
            onClick = onRefresh,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Scan PDF Files")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF808080))
        ) {
            items(pdfFiles) { pdf ->
                PdfListItem(
                    pdf = pdf,
                    onClick = {
                        selectedPdf = pdf
                    },
                    onMoreClick = {
                        selectedPdf = pdf
                        showSheet = true
                    }
                )
            }
        }

        // 底部菜单弹窗
        if (showSheet && selectedPdf != null) {
            PdfActionSheet(
                pdf = selectedPdf!!,
                sheetState = sheetState,
                onDismiss = { showSheet = false }
            )
        }
    }
}
