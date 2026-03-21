package com.quantumstudio.smartpdf.ui.features.reader.components

import android.app.Activity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quantumstudio.smartpdf.ui.features.reader.ReaderUiState

@Composable
fun BrightnessSliderLayout(activity: Activity?, uiState: ReaderUiState) {

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        // 顶部标签栏
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // 左侧小太阳
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(14.dp)
            )
            // 中间文字
            Text(
                text = "Brightness",
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.9f), // 使用你截图中的紫色
                fontSize = 12.sp,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 12.dp)
            )
            // 右侧大太阳
            Icon(
                imageVector = Icons.Default.WbSunny,
                contentDescription = null,
                tint = Color.Gray,
                modifier = Modifier.size(18.dp)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Slider(
            value = if (uiState.currentBrightness < 0f) 0.5f else uiState.currentBrightness,
            onValueChange = { newValue ->
                uiState.updateBrightness(newValue)
                activity?.let {
                    val lp = it.window.attributes
                    lp.screenBrightness = newValue
                    it.window.attributes = lp
                }
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}