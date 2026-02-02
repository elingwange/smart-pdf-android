package com.quantumstudio.smartpdf

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.quantumstudio.smartpdf.ui.screens.MainScreen
import com.quantumstudio.smartpdf.ui.theme.SmartPDFTheme
import com.quantumstudio.smartpdf.ui.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {

    // 使用 viewModels() 委托获取实例
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        // 1. 开启沉浸式状态栏（让深色背景充满屏幕）
        enableEdgeToEdge()

        super.onCreate(savedInstanceState)

        setContent {
            // 2. 使用项目的主题包围（确保字体和基础颜色配置生效）
            SmartPDFTheme {
                // 3. 使用 Surface 作为底层容器，防止背景透明或显示异常
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // 4. 加载你定义的 MainScreen
                    MainScreen()
                }
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        // 每次从系统设置页回来，自动检查一遍权限
        viewModel.checkPermission(this)
    }
}
