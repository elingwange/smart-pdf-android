package com.quantumstudio.smartpdf.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.NorthEast
import androidx.compose.material.icons.filled.SouthEast
import androidx.compose.material.icons.filled.TextFields
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quantumstudio.smartpdf.data.model.SortField
import com.quantumstudio.smartpdf.data.model.SortOrder


@Composable
fun SortByDialog(
    currentField: SortField,
    currentOrder: SortOrder,
    onDismiss: () -> Unit,
    onConfirm: (SortField, SortOrder) -> Unit
) {
    var tempField by remember { mutableStateOf(currentField) }
    var tempOrder by remember { mutableStateOf(currentOrder) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Sort by", fontWeight = FontWeight.Bold) },
        text = {
            Column {
                SortOptionItem(
                    Icons.Default.CalendarToday,
                    "Last modified",
                    tempField == SortField.DATE
                ) { tempField = SortField.DATE }
                SortOptionItem(
                    Icons.Default.TextFields,
                    "Name",
                    tempField == SortField.NAME
                ) { tempField = SortField.NAME }
                SortOptionItem(
                    Icons.Default.Description,
                    "File Size",
                    tempField == SortField.SIZE
                ) { tempField = SortField.SIZE }

                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                SortOptionItem(
                    Icons.Default.SouthEast,
                    "Ascending",
                    tempOrder == SortOrder.ASCENDING
                ) { tempOrder = SortOrder.ASCENDING }
                SortOptionItem(
                    Icons.Default.NorthEast,
                    "Descending",
                    tempOrder == SortOrder.DESCENDING
                ) { tempOrder = SortOrder.DESCENDING }
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(tempField, tempOrder) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF44336)) // 使用截图中的红色
            ) { Text("OK") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun SortOptionItem(icon: ImageVector, label: String, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (selected) Color(0xFFF44336) else Color.Gray
        )
        Spacer(Modifier.width(16.dp))
        Text(
            label,
            color = if (selected) Color(0xFFF44336) else MaterialTheme.colorScheme.onSurface
        )
    }
}