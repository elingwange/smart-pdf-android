plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    kotlin("kapt")
    id("com.google.dagger.hilt.android")
}

android {
    namespace = "com.quantumstudio.smartpdf"
    compileSdk = 35
    buildToolsVersion = "35.0.0"

    defaultConfig {
        applicationId = "com.quantumstudio.smartpdf"
        minSdk = 23
        targetSdk = 35
        versionCode = 600
        versionName = "0.6.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = libs.versions.composeCompiler.get()
    }
}


dependencies {
    // ---------------------------------------------------------
    // 1. AndroidX 核心与 Compose 基础 (受 BOM 自动管理版本)
    // ---------------------------------------------------------
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // 状态采集与生命周期 (建议统一使用 libs 引用)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // ---------------------------------------------------------
    // 2. 依赖注入 (Hilt 推荐使用 2.51.1+ 以获得最佳 API 35 支持)
    // ---------------------------------------------------------
    implementation("com.google.dagger:hilt-android:2.51.1")
    kapt("com.google.dagger:hilt-compiler:2.51.1")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // ---------------------------------------------------------
    // 3. 导航、存储与多媒体
    // ---------------------------------------------------------
    // 导航 (2.8.5 已支持类型安全和 Android 15 预测性返回)
    implementation("androidx.navigation:navigation-compose:2.8.5")

    // Room 数据库 (2.6.1 是目前的稳定版)
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    kapt("androidx.room:room-compiler:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")

    // DataStore 与 协程
    implementation(libs.androidx.datastore.preferences)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.0")

    // PDF 阅读与解析
    implementation(project(":android-pdf-viewer"))
    implementation("com.tom-roush:pdfbox-android:2.0.27.0")

    // ---------------------------------------------------------
    // 4. UI 增强 (图标、启动页)
    // ---------------------------------------------------------
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.core:core-splashscreen:1.0.1")

    // ---------------------------------------------------------
    // 5. 测试相关
    // ---------------------------------------------------------
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)

    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}