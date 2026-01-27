package com.quantumstudio.smartpdf

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
import com.quantumstudio.smartpdf.pdf.import2.PDFImportManager
import com.quantumstudio.smartpdf.ui.screens.PDFImportScreen
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var pdfImportManager: PDFImportManager
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>
    private val importedFiles = mutableStateListOf<File>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        pdfImportManager = PDFImportManager(this)

        // 注册 Launcher
        filePickerLauncher = registerForActivityResult(
            androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
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

        // 将 Launcher 注入 Manager
        pdfImportManager.filePickerLauncher = filePickerLauncher

        setContent {
            MaterialTheme {
                PDFImportScreen(
                    onSelectPdf = { pdfImportManager.openFilePicker() },
                    importedFiles = importedFiles
                )
            }
        }
    }
}
