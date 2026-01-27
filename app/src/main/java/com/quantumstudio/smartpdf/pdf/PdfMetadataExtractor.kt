
import com.quantumstudio.smartpdf.pdf.PdfFile
import com.tom_roush.pdfbox.pdmodel.PDDocument
import java.io.File

object PdfMetadataExtractor {

    fun extractMetadata(file: File): PdfFile {
        val document = PDDocument.load(file)
        val pageCount = document.numberOfPages
        document.close()

        return PdfFile(
            name = file.name,
            path = file.absolutePath,
            size = file.length(),
            pages = pageCount
        )
    }
}
