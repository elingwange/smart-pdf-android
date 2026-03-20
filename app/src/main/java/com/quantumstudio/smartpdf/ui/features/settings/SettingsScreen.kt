package com.quantumstudio.smartpdf.ui.features.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.quantumstudio.smartpdf.ui.components.DefaultAppGuideDialog
import com.quantumstudio.smartpdf.ui.components.LanguageSelectionDialog
import com.quantumstudio.smartpdf.ui.components.ThemeSelectionDialog
import com.quantumstudio.smartpdf.util.CommonUtils
import com.quantumstudio.smartpdf.util.CommonUtils.openAppInfoSettings
import com.quantumstudio.smartpdf.util.CommonUtils.openPlayStore
import com.quantumstudio.smartpdf.util.CommonUtils.openPrivacyPolicy
import com.quantumstudio.smartpdf.util.CommonUtils.openSystemFileManager
import com.quantumstudio.smartpdf.util.CommonUtils.sendFeedbackEmail


// 对话框类型
enum class SettingsDialog {
    THEME, LANGUAGE, DEFAULT_GUIDE
}

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {

    val context = LocalContext.current
    val appVersion = remember { CommonUtils.getAppVersionName(context) }
    val currentTheme by viewModel.themeMode.collectAsState()
    val currentLanguage by viewModel.currentLanguage.collectAsState(initial = "system")

    var activeDialog by rememberSaveable { mutableStateOf<SettingsDialog?>(null) }
    // 统一分发逻辑
    when (activeDialog) {
        SettingsDialog.THEME -> ThemeSelectionDialog(
            currentTheme = currentTheme,
            onDismiss = { activeDialog = null },
            onSelect = { viewModel.setThemeMode(it); activeDialog = null }
        )

        SettingsDialog.LANGUAGE -> LanguageSelectionDialog(
            currentLanguage = currentLanguage,
            onDismiss = { activeDialog = null },
            onSelect = { viewModel.setLanguage(it); activeDialog = null }
        )

        SettingsDialog.DEFAULT_GUIDE -> DefaultAppGuideDialog(
            onDismiss = { activeDialog = null },
            onConfirm = { openAppInfoSettings(context); activeDialog = null }
        )

        null -> {}
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background) // 跟主背景保持一致
            .padding(16.dp)
            .verticalScroll(rememberScrollState()) // 支持长页面滚动
    ) {
        SettingSection(title = "General") {
            SettingRow(Icons.Outlined.Folder, "File Manager") { openSystemFileManager(context) }
            SettingDivider()
            SettingRow(Icons.Outlined.Palette, "Theme Mode") { activeDialog = SettingsDialog.THEME }
            SettingDivider()
            SettingRow(Icons.Outlined.SettingsSuggest, "Set as Default") {
                activeDialog = SettingsDialog.DEFAULT_GUIDE
            }
            SettingDivider()
            SettingRow(Icons.Outlined.Language, "Language") {
                activeDialog = SettingsDialog.LANGUAGE
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        SettingSection(title = "About") {
            SettingRow(Icons.Outlined.ThumbUpOffAlt, "Rate us") { openPlayStore(context) }
            SettingDivider()
            SettingRow(Icons.Outlined.Feedback, "Feedback") { sendFeedbackEmail(context) }
            SettingDivider()
            SettingRow(Icons.Outlined.PrivacyTip, "Privacy policy") { openPrivacyPolicy(context) }
            SettingDivider()
            SettingRow(Icons.Outlined.Info, "Version", subtitle = appVersion) {}
        }
    }
}

@Composable
fun SettingSection(
    title: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier = Modifier.padding(top = 16.dp)) {
        if (title != null) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(start = 8.dp, bottom = 8.dp)
            )
        }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(12.dp),
            content = content
        )
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
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )
}
