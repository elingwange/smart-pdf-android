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
import com.quantumstudio.smartpdf.ui.screens.BottomTabBar
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var pdfImportManager: PDFImportManager
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

    // Compose 可观察的 PDF 文件列表
    private val importedFiles = mutableStateListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        initPdfImport()

        setContent {
            MaterialTheme {
                BottomTabBar(
                    importedFiles = importedFiles,
                    onSelectPdf = { pdfImportManager.openFilePicker() },
                    onOpenPdf = { file -> openPdf(file) }
                )
            }
        }
    }

    private fun initPdfImport() {
        pdfImportManager = PDFImportManager(this)

        filePickerLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                val uris = mutableListOf<Uri>()
                result.data?.data?.let { uris.add(it) }
                result.data?.clipData?.let { clip ->
                    for (i in 0 until clip.itemCount) {
                        uris.add(clip.getItemAt(i).uri)
                    }
                }
                pdfImportManager.handleImportedUris(uris) { files ->
                    importedFiles.addAll(files)
                }
            }
        }

        pdfImportManager.filePickerLauncher = filePickerLauncher
    }

    private fun openPdf(file: File) {
//        val intent = Intent(this, PDFViewerActivity::class.java)
//        intent.putExtra("pdf_uri", Uri.fromFile(file))
        val intent = Intent(this, PDFViewerActivity::class.java).apply {
            putExtra("pdf_uri", Uri.fromFile(file))
        }
        startActivity(intent)
    }
}
