package com.quantumstudio.smartpdf.ui.features.main.components


import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.quantumstudio.smartpdf.data.model.SortField
import com.quantumstudio.smartpdf.data.model.SortOrder

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SortByBottomSheet(
    currentField: SortField,
    currentOrder: SortOrder,
    onDismiss: () -> Unit,
    onConfirm: (SortField, SortOrder) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() } // 显示顶部的横条
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp) // 留出底部导航栏的安全距离
        ) {
            Text(
                text = "排序方式",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )

            // 排序字段选择
            SortOptionItem("按日期", currentField == SortField.DATE) {
                onConfirm(SortField.DATE, currentOrder)
            }
            SortOptionItem("按名称", currentField == SortField.NAME) {
                onConfirm(SortField.NAME, currentOrder)
            }
            SortOptionItem("按大小", currentField == SortField.SIZE) {
                onConfirm(SortField.SIZE, currentOrder)
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // 升降序切换
            Text(
                text = "顺序",
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(16.dp)
            )
            SortOptionItem("降序 (从新到旧/从大到小)", currentOrder == SortOrder.DESCENDING) {
                onConfirm(currentField, SortOrder.DESCENDING)
            }
            SortOptionItem("升序 (从旧到新/从小到大)", currentOrder == SortOrder.ASCENDING) {
                onConfirm(currentField, SortOrder.ASCENDING)
            }
        }
    }
}

@Composable
private fun SortOptionItem(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
        )
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}