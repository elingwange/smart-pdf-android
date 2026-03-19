package com.quantumstudio.smartpdf.data.repository


import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.quantumstudio.smartpdf.ui.features.settings.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

// 定义 DataStore 扩展属性
private val Context.dataStore by preferencesDataStore(name = "settings")

class ThemeRepository @Inject constructor(@ApplicationContext private val context: Context) {

    private val THEME_KEY = stringPreferencesKey("theme_mode")

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
}