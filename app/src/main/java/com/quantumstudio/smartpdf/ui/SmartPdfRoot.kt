package com.quantumstudio.smartpdf.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.quantumstudio.smartpdf.ui.features.settings.SettingsViewModel
import com.quantumstudio.smartpdf.ui.theme.SmartPDFTheme

@Composable
fun SmartPDFRoot(
    settingsViewModel: SettingsViewModel,
    navController: NavHostController = rememberNavController(),
    // 改用 Lambda 传出 controller，让 Activity 接收
    onCreated: (NavHostController) -> Unit,
    // content 接收一个 controller 参数
    content: @Composable (NavHostController) -> Unit
) {
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