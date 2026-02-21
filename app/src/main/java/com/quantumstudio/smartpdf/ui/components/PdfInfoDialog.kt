import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumstudio.smartpdf.data.model.PdfFile
import com.quantumstudio.smartpdf.util.CommonUtils.formatTimestamp
import com.quantumstudio.smartpdf.util.FileUtils.formatFileSize

@Composable
fun PdfInfoDialog(
    pdf: PdfFile,
    onDismiss: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(28.dp),
            // ✨ 使用 surfaceContainer 或 surfaceVariant 替代硬编码灰色
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            // 适当增加阴影以增强悬浮感
            shadowElevation = 6.dp
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Details",
                    fontSize = 20.sp,
                    // ✨ 使用 onSurface 确保文字颜色随背景自动切换
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )

                InfoItem("Name", pdf.name)
                InfoItem("Size", formatFileSize(pdf.size))
                InfoItem("Path", pdf.path)
                InfoItem("Last modified", formatTimestamp(pdf.lastModified))

                Spacer(modifier = Modifier.size(8.dp))

                androidx.compose.material3.Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        // ✨ 按钮背景使用 primary，文字使用 onPrimary
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("OK")
                }
            }
        }
    }
}

@Composable
private fun InfoItem(label: String, value: String) {
    Column {
        Text(
            text = label,
            // ✨ 使用 onSurfaceVariant 或 secondary 表达副标题
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            fontSize = 14.sp
        )
        Text(
            text = value,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}