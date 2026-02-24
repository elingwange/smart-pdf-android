package com.quantumstudio.smartpdf.ui.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Feedback
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.SettingsSuggest
import androidx.compose.material.icons.outlined.ThumbUpOffAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.quantumstudio.smartpdf.ui.features.main.MainViewModel
import com.quantumstudio.smartpdf.ui.features.main.ThemeMode
import com.quantumstudio.smartpdf.util.CommonUtils.openSystemFileManager

@Composable
fun SettingsScreen(viewModel: MainViewModel) {// 观察当前主题状态（用于显示 subtitle 和 RadioButton 选中状态）
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentTheme by viewModel.themeMode.collectAsState()
    // 控制对话框显示的开关
    var showThemeDialog by remember { mutableStateOf(false) }
    // 第二步：根据状态判断是否显示对话框
    if (showThemeDialog) {
        ThemeSelectionDialog(
            currentTheme = currentTheme,
            onDismiss = { showThemeDialog = false }, // 点击外部消失
            onSelect = { selectedMode ->
                viewModel.setThemeMode(selectedMode) // 2. 调用 ViewModel 更新状态
                showThemeDialog = false // 3. 选完后关闭
            }
        )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // 跟主背景保持一致
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // 支持长页面滚动
    ) {
        // 第一组：通用设置
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // 稍浅的卡片背景
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                SettingRow(Icons.Outlined.Folder, "File Manager") { /* 处理点击 */
                    openSystemFileManager(context)
                }
                SettingDivider()
                SettingRow(
                    icon = Icons.Outlined.Palette,
                    title = "Theme Mode"
                ) { showThemeDialog = true }
                SettingDivider()
                SettingRow(Icons.Outlined.SettingsSuggest, "Set as Default") { /* 处理点击 */ }
                // 在 SettingRow 之间插入
                SettingDivider()
                SettingRow(Icons.Outlined.Language, "Language") { /* 处理点击 */ }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 第二组：关于与反馈
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column {
                SettingRow(Icons.Outlined.ThumbUpOffAlt, "Rate us") { /* 处理点击 */ }
                SettingDivider()
                SettingRow(Icons.Outlined.Feedback, "Feedback") { /* 处理点击 */ }
                SettingDivider()
                SettingRow(Icons.Outlined.PrivacyTip, "Privacy policy") { /* 处理点击 */ }
                SettingDivider()
                SettingRow(
                    icon = Icons.Outlined.Info,
                    title = "Version",
                    subtitle = "6.3.2"
                ) { /* 处理点击 */ }
            }
        }

    }
}

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

@Composable
fun SettingRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent, // 保持透明，显示 Card 的颜色
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 16.dp, vertical = 20.dp) // 稍微加宽垂直内边距，更有呼吸感
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                // 💡 建议：图标颜色可以用 primary 或者是稍微弱化一点的 onSurface
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (subtitle != null) {
                Spacer(modifier = Modifier.weight(1f))
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    // 💡 这里你用对了！onSurfaceVariant 是次要信息的最佳选择
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                // 如果没有副标题，通常加一个右指箭头 ">"
                Spacer(modifier = Modifier.weight(1f))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.outline,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun SettingDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(horizontal = 16.dp),
        thickness = 0.5.dp,
        // 使用 outlineVariant 是 M3 的标准做法
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}