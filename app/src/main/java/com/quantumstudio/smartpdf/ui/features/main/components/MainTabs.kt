package com.quantumstudio.smartpdf.ui.features.main.components

import PdfInfoDialog
import android.net.Uri
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.ui.features.main.MainViewModel
import com.quantumstudio.smartpdf.util.CommonUtils.sharePdf
import java.io.File


@Composable
fun AllFilesTab(viewModel: MainViewModel, onFileClick: (Uri) -> Unit) {
    val files by viewModel.sortedPdfFiles.collectAsState()
    PdfListContent(files, viewModel, onFileClick)
}

@Composable
fun FavoriteFilesTab(viewModel: MainViewModel, onFileClick: (Uri) -> Unit) {
    val files by viewModel.sortedPdfFiles.collectAsState()
    val favoriteFiles = remember(files) { files.filter { it.isFavorite } }
    PdfListContent(favoriteFiles, viewModel, onFileClick)
}

@Composable
fun RecentFilesTab(viewModel: MainViewModel, onFileClick: (Uri) -> Unit) {
    val files by viewModel.allPdfsFlow.collectAsState()
    val recentFiles = remember(files) {
        files.filter { it.lastReadTime > 0 }.sortedByDescending { it.lastReadTime }
    }
    PdfListContent(recentFiles, viewModel, onFileClick)
}


@Composable
fun PdfListContent(
    files: List<PdfFile>,
    viewModel: MainViewModel,
    onFileClick: (Uri) -> Unit
) {
    val context = LocalContext.current
    var selectedPdfForInfo by remember { mutableStateOf<PdfFile?>(null) }
    var pdfToDelete by remember { mutableStateOf<PdfFile?>(null) }
    var pdfToRename by remember { mutableStateOf<PdfFile?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (files.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No PDF files found", color = Color.Gray)
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(items = files, key = { it.path }) { pdf ->
                    PdfListItem(
                        pdf = pdf,
                        onClick = { onFileClick(Uri.fromFile(File(pdf.path))) },
                        onMenuAction = { action ->
                            when (action) {
                                is MenuAction.Info -> selectedPdfForInfo = pdf
                                is MenuAction.Favorite -> viewModel.toggleFavorite(pdf)
                                is MenuAction.Rename -> pdfToRename = pdf
                                is MenuAction.Delete -> pdfToDelete = pdf
                                is MenuAction.Share -> sharePdf(context, pdf)
                                else -> {}
                            }
                        }
                    )
                }
            }
        }

        selectedPdfForInfo?.let { pdf ->
            PdfInfoDialog(pdf = pdf, onDismiss = { selectedPdfForInfo = null })
        }

        pdfToDelete?.let { pdf ->
            PdfDeleteDialog(
                fileName = pdf.name,
                onDismiss = { pdfToDelete = null },
                onConfirm = {
                    viewModel.deleteFile(pdf)
                    pdfToDelete = null
                }
            )
        }

        pdfToRename?.let { pdf ->
            PdfRenameDialog(
                currentName = pdf.name,
                onDismiss = { pdfToRename = null },
                onConfirm = { newName ->
                    viewModel.renameFile(pdf, newName)
                    pdfToRename = null
                }
            )
        }
    }
}
