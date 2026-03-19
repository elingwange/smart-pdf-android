package com.quantumstudio.smartpdf.ui.features.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quantumstudio.smartpdf.data.repository.ThemeRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

@HiltViewModel
class SettingsViewModel @Inject constructor(private val themeRepository: ThemeRepository) :
    ViewModel() {

    // 初始值设为 SYSTEM
    val themeMode = themeRepository.themeModeFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ThemeMode.SYSTEM
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themeRepository.saveThemeMode(mode)
        }
    }
}