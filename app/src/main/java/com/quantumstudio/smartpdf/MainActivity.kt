package com.quantumstudio.smartpdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.quantumstudio.smartpdf.ui.screens.AiChatScreen
import com.quantumstudio.smartpdf.ui.screens.HomeScreen
import com.quantumstudio.smartpdf.ui.screens.ToolboxScreen
import com.quantumstudio.smartpdf.ui.theme.SmartPDFTheme


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SmartPDFTheme {
                MainNavigationScreen()
            }
        }
    }
}

@Composable
fun MainNavigationScreen() {
    // 1. 状态管理：记录当前选中的标签页
    var selectedTab by remember { mutableIntStateOf(0) }
    val items = listOf("Files", "AI", "Settings")
    val icons = listOf(Icons.Default.List, Icons.Default.Face, Icons.Default.Settings)

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        // 2. 底部导航栏
        bottomBar = {
            NavigationBar {
                items.forEachIndexed { index, item ->
                    NavigationBarItem(
                        icon = { Icon(icons[index], contentDescription = item) },
                        label = { Text(item) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        // 3. 根据选中的标签切换页面
        Box(modifier = Modifier.padding(innerPadding)) {
            when (selectedTab) {
                0 -> HomeScreen()
                1 -> ToolboxScreen()
                2 -> AiChatScreen()
            }
        }
    }
}

// 预览普通手机
//@Preview(name = "Phone", showSystemUi = true, device = Devices.PIXEL_4)
// 暗黑模式预览
@Preview(
    name = "Dark Mode",
    showBackground = true,
    uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES
)
// 预览平板（查看导航栏是否显得太宽）
//@Preview(name = "Tablet", showSystemUi = true, device = Devices.PIXEL_C)
@Composable
fun MultiDevicePreview() {
    SmartPDFTheme {
        MainNavigationScreen()
    }
}