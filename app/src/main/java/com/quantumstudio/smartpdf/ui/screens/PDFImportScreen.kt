package com.quantumstudio.smartpdf.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.io.File

@Composable
fun PDFImportScreen(
    onSelectPdf: () -> Unit,
    importedFiles: List<File>
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // 按钮：选择本地 PDF
        Button(
            onClick = onSelectPdf,
            modifier = Modifier
                .fillMaxWidth()
        ) {
            Text(text = "选择本地 PDF")
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 已导入 PDF 列表标题
        Text(
            text = "已导入 PDF:",
            style = MaterialTheme.typography.titleMedium
        )

        Spacer(modifier = Modifier.height(8.dp))

        // 列表显示导入的文件名
        LazyColumn(
            modifier = Modifier.fillMaxSize()
        ) {
            items(importedFiles) { file ->
                Text(
                    text = file.name,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(4.dp)
                )
            }
        }
    }
}
