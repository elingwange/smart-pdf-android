package com.quantumstudio.smartpdf.pdf.viewer

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import com.github.barteksc.pdfviewer.PDFView

class PDFViewerActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val pdfView = PDFView(this, null)
        setContentView(pdfView)

        val pdfUri = intent.getParcelableExtra<Uri>("pdf_uri") ?: return

        pdfView.fromUri(pdfUri)
            .enableSwipe(true) // 支持滑动翻页
            .swipeHorizontal(false)
            .enableAnnotationRendering(false)
            .enableDoubletap(true)
            .defaultPage(0)
            .load()
    }
}
