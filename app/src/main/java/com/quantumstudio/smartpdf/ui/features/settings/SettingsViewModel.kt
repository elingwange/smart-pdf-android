package com.quantumstudio.smartpdf.ui.features.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantumstudio.smartpdf.data.repository.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: SettingsRepository) :
    ViewModel() {

    // 初始值设为 SYSTEM
    val themeMode = repository.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            repository.saveThemeMode(mode)
        }
    }


    // UI 观察这个状态
    val currentLanguage = repository.languageFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "system")

    fun setLanguage(langCode: String) {
        viewModelScope.launch {
            repository.saveLanguage(langCode)
            // ✨ 核心：立即应用语言变更（无需重启 App）
            applyLanguage(langCode)
        }
    }

    private fun applyLanguage(langCode: String) {
        val appLocale: LocaleListCompat = if (langCode == "system") {
            LocaleListCompat.getEmptyLocaleList()
        } else {
            LocaleListCompat.forLanguageTags(langCode)
        }
        // AppCompatDelegate 会自动处理多 Activity 的语言同步
        AppCompatDelegate.setApplicationLocales(appLocale)
    }
}