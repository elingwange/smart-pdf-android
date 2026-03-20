package com.quantumstudio.smartpdf.ui.features.settings.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quantumstudio.smartpdf.ui.features.settings.ThemeMode


@Composable
fun ThemeSelectionDialog(
    currentTheme: ThemeMode,
    onDismiss: () -> Unit,
    onSelect: (ThemeMode) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Choose Theme") },
        confirmButton = {}, // M3 规范列表选择通常不需要确认按钮，点选即关闭
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                ThemeOption("Follow System", currentTheme == ThemeMode.SYSTEM) {
                    onSelect(ThemeMode.SYSTEM)
                }
                ThemeOption("Light", currentTheme == ThemeMode.LIGHT) {
                    onSelect(ThemeMode.LIGHT)
                }
                ThemeOption("Dark", currentTheme == ThemeMode.DARK) {
                    onSelect(ThemeMode.DARK)
                }
            }
        }
    )
}


@Composable
fun ThemeOption(text: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = null) // onClick 为空，因为 Row 处理了点击
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge)
    }
}