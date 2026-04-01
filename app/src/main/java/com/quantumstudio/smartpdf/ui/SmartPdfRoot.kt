package com.quantumstudio.smartpdf.ui

import androidx.appcompat.app.AppCompatDelegate
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.os.LocaleListCompat
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.quantumstudio.smartpdf.ui.features.settings.SettingsViewModel
import com.quantumstudio.smartpdf.ui.theme.SmartPDFTheme

@Composable
fun SmartPDFRoot(
    settingsViewModel: SettingsViewModel,
    navController: NavHostController = rememberNavController(),
    onCreated: (NavHostController) -> Unit,
    content: @Composable (NavHostController) -> Unit
) {

    val currentLanguage by settingsViewModel.currentLanguage.collectAsState()
    LaunchedEffect(currentLanguage) {

        // 确保每次语言状态改变时，通知系统应用 Locale
        val appLocale = if (currentLanguage == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(currentLanguage)
        }
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
    // 当控制器创建好后，通知外部（Activity）
    LaunchedEffect(navController) {
        onCreated(navController)
    }
    val themeMode by settingsViewModel.themeMode.collectAsState()
    SmartPDFTheme(themeMode) {
        Surface(modifier = Modifier.fillMaxSize()) {
            // 把内部创建的 controller 交还给具体的导航配置
            content(navController)
        }
    }
}