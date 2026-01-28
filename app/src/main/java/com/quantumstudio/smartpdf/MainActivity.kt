package com.quantumstudio.smartpdf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
import com.quantumstudio.smartpdf.pdf.import2.PDFImportManager
import com.quantumstudio.smartpdf.pdf.viewer.PDFViewerActivity
import com.quantumstudio.smartpdf.ui.screens.PDFImportScreen
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var pdfImportManager: PDFImportManager
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

    // Compose 可观察的 PDF 文件列表
    private val importedFiles = mutableStateListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pdfImportManager = PDFImportManager(this)

        // 注册文件选择 Launcher
        filePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val uris = mutableListOf<Uri>()

                // 单选
                result.data?.data?.let { uris.add(it) }

                // 多选
                result.data?.clipData?.let { clip ->
                    for (i in 0 until clip.itemCount) {
                        uris.add(clip.getItemAt(i).uri)
                    }
                }

                // 处理导入的 PDF
                pdfImportManager.handleImportedUris(uris) { files ->
                    importedFiles.addAll(files)
                }
            }
        }

        // 注入 Launcher
        pdfImportManager.filePickerLauncher = filePickerLauncher

        setContent {
            MaterialTheme {
                PDFImportScreen(
                    onSelectPdf = {
                        pdfImportManager.openFilePicker()
                    },
                    importedFiles = importedFiles,
                    onOpenPdf = { file ->
                        openPdf(file)
                    }
                )
            }
        }
    }

    private fun openPdf(file: File) {
        val intent = Intent(this, PDFViewerActivity::class.java).apply {
            putExtra("pdf_uri", Uri.fromFile(file))
        }
        startActivity(intent)
    }
}
