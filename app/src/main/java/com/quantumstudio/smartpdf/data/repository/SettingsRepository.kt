package com.quantumstudio.smartpdf.data.repository


import android.content.Context
import androidx.datastore.core.IOException
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.quantumstudio.smartpdf.ui.features.settings.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// 定义 DataStore 扩展属性
private val Context.dataStore by preferencesDataStore(name = "settings")

class SettingsRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val THEME_KEY = stringPreferencesKey("theme_mode")

    private val LANGUAGE_KEY = stringPreferencesKey("selected_language")

    // 默认语言建议设为 "system" 或 "en"
    val DEFAULT_LANGUAGE = "system"

    // 读取主题设置
    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val name = preferences[THEME_KEY] ?: ThemeMode.SYSTEM.name
        ThemeMode.valueOf(name)
    }

    // 保存主题设置
    suspend fun saveThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[THEME_KEY] = mode.name
        }
    }

    // 1. 获取语言设置（响应式）
    val languageFlow: Flow<String> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences())
            else throw exception
        }
        .map { preferences ->
            preferences[LANGUAGE_KEY] ?: DEFAULT_LANGUAGE
        }

    // 2. 保存语言设置
    suspend fun saveLanguage(langCode: String) {
        context.dataStore.edit { preferences ->
            preferences[LANGUAGE_KEY] = langCode
        }
    }
}