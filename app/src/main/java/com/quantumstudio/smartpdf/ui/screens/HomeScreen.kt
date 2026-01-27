package com.quantumstudio.smartpdf.ui.screens

import android.content.pm.PackageManager
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quantumstudio.smartpdf.model.PdfFile
import com.quantumstudio.smartpdf.ui.viewmodel.HomeViewModel

@Composable
fun HomeScreen(homeViewModel: HomeViewModel = viewModel()) {
    val context = LocalContext.current

    // 1. 定义权限请求启动器
    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            homeViewModel.loadFiles() // 授权成功，开始扫描
        } else {
            Toast.makeText(context, "需要存储权限才能读取PDF", Toast.LENGTH_SHORT).show()
        }
    }

    // 2. 检查当前权限状态
    val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        android.Manifest.permission.READ_MEDIA_IMAGES // 示例：Android 13+
    } else {
        android.Manifest.permission.READ_EXTERNAL_STORAGE
    }

    val hasPermission = ContextCompat.checkSelfPermission(
        context, permission
    ) == PackageManager.PERMISSION_GRANTED

    // 自动检查逻辑
    LaunchedEffect(Unit) {
        if (hasPermission) {
            homeViewModel.loadFiles()
        }
    }

    // 3. UI 逻辑
    Box(modifier = Modifier.fillMaxSize()) {
        if (hasPermission) {
            // 已授权：显示列表
            PdfListContent(homeViewModel)
            // 进入页面自动刷新一次
            LaunchedEffect(Unit) { homeViewModel.loadFiles() }
        } else {
            // 未授权：显示引导界面
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("SmartPDF 需要访问您的文件以进行阅读")
                Button(onClick = { permissionLauncher.launch(permission) }) {
                    Text("授予权限")
                }
            }
        }
    }
}


@Composable
fun PdfListContent(homeViewModel: HomeViewModel) {
    val pdfFiles = homeViewModel.pdfFiles
    val isLoading = homeViewModel.isLoading

    Box(modifier = Modifier.fillMaxSize()) {
        if (isLoading) {
            // 加载中状态
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
        } else if (pdfFiles.isEmpty()) {
            // 空状态：没扫描到 PDF
            Column(
                modifier = Modifier.align(Alignment.Center),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(64.dp), tint = Color.Gray)
                Text("未发现 PDF 文件", color = Color.Gray, modifier = Modifier.padding(top = 16.dp))
            }
        } else {
            // 真正的数据列表
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(pdfFiles) { file ->
                    PdfFileCard(file)
                }
            }
        }
    }
}

@Composable
fun PdfFileCard(file: PdfFile) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(text = file.name, style = MaterialTheme.typography.bodyLarge, maxLines = 1)
                Text(
                    text = "${(file.size / 1024 / 1024)} MB  |  ${file.path.substringAfterLast("/")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        }
    }
}