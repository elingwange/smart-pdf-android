package com.quantumstudio.smartpdf.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext


private val DarkColorScheme = darkColorScheme(
    primary = PdfRed,
    onPrimary = Color.White,
    background = PdfBackgroundDark,
    onBackground = Color.White,
    surface = PdfSurfaceDark,
    onSurface = Color.White,

    // 💡 改进点：专门用于导航栏未选中、副标题、分割线
    surfaceVariant = Color(0xFF2C2C2C),   // 容器色
    onSurfaceVariant = Color(0xFFBDBDBD), // 内容色（这个就是你想要的“亮一点的灰”）
    outline = Color(0xFF757575)           // 边框或禁用色
)

private val LightColorScheme = lightColorScheme(
    primary = PdfRed,
    onPrimary = Color.White,
    background = PdfBackgroundLight,
    onBackground = Color.Black,
    surface = PdfSurfaceLight,
    onSurface = Color.Black,

    surfaceVariant = Color(0xFFF0F00),
    onSurfaceVariant = Color(0xFF616161)
)

@Composable
fun SmartPDFTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    // Dynamic color is available on Android 12+
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}