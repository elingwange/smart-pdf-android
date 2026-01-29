package com.quantumstudio.smartpdf

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateListOf
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.quantumstudio.smartpdf.pdf.data.PdfFile
import com.quantumstudio.smartpdf.pdf.data.PdfScanner
import com.quantumstudio.smartpdf.pdf.import2.PDFImportManager
import com.quantumstudio.smartpdf.pdf.viewer.PDFViewerActivity
import com.quantumstudio.smartpdf.ui.screens.FilesScreen2
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class MainActivity : ComponentActivity() {

    private lateinit var pdfImportManager: PDFImportManager
    private lateinit var filePickerLauncher: ActivityResultLauncher<Intent>

    // Compose 可观察的 PDF 文件列表
    private val importedFiles = mutableStateListOf<File>()

    private val pdfFiles = mutableStateListOf<PdfFile>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!hasAllFilesAccess())
            requestAllFilesAccess(this)
        else {
            // 已有权限，扫描所有pdf文件
            setContent {
                MaterialTheme {
                    FilesScreen2(
                        pdfFiles = pdfFiles,
                        onOpenPdf = { pdf -> openPdfFile(pdf) },
                        onRefresh = { scanPdfs() }
                    )
                }
            }
        }


//        initPdfImport()

//        setContent {
//            MaterialTheme {
//                BottomTabBar(
//                    importedFiles = importedFiles,
//                    onSelectPdf = { pdfImportManager.openFilePicker() },
//                    onOpenPdf = { file -> openPdf(file) }
//                )
//            }
//        }
    }

    private fun scanPdfs() {
        // 使用协程在后台扫描
        lifecycleScope.launch {
            val scanned = withContext(Dispatchers.IO) {
                PdfScanner.scanAllPdfFiles(this@MainActivity)
            }
            pdfFiles.clear()
            pdfFiles.addAll(scanned)
        }
    }

    private fun requestAllFilesAccess(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val intent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
            ).apply {
                data = Uri.parse("package:${context.packageName}")
            }
            context.startActivity(intent)
        }
    }

    fun hasAllFilesAccess(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED
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

    private fun openPdfFile(pdf: PdfFile) {
        val intent = Intent(this, PDFViewerActivity::class.java).apply {
            putExtra("pdf_uri", Uri.fromFile(File(pdf.path)))
        }
        startActivity(intent)
    }

    private fun openPdf(file: File) {
        val intent = Intent(this, PDFViewerActivity::class.java).apply {
            putExtra("pdf_uri", Uri.fromFile(file))
        }
        startActivity(intent)
    }
}
