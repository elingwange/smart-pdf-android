# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile


# Hilt 规则
-keep class com.google.dagger.hilt.** { *; }

# Room 规则
-keep class * extends androidx.room.RoomDatabase
-keep class * extends androidx.room.Entity
-keep class * extends androidx.room.Dao

# PDFBox 规则 (非常关键)
-keep class com.tom_roush.pdfbox.** { *; }
-dontwarn com.tom_roush.pdfbox.**

# 保存 Compose 混淆兼容
-keep class androidx.compose.material.icons.** { *; }

# 保护 Baseline Profile 安装器
-keep class androidx.profileinstaller.** { *; }
-dontwarn androidx.profileinstaller.**

# 如果你使用了特定的启动页优化
-keep class androidx.tracing.** { *; }

# 保护所有数据模型类
-keep class com.quantumstudio.smartpdf.data.model.** { *; }
# 使用了 Room 也要保护
-keep @androidx.room.Entity class * { *; }

#移除所有 Log 调用
-assumenosideeffects class android.util.Log {
    public static *** d(...);
    public static *** v(...);
    public static *** i(...);
}


# 不要 keep 整个 org.bouncycastle
# 只保留必须的部分（如果 PDFBox 报错再按需添加）
-dontwarn org.bouncycastle.**
# 既然已经有了 -dontwarn，我们尝试直接移除未使用的 pqc 包（如果 PDFBox 运行正常）
-keep class org.bouncycastle.jcajce.provider.asymmetric.util.** { *; }
# 过滤掉不需要的加密算法协议，仅保留 PDF 签名验证可能用到的核心
-dontnote org.bouncycastle.**