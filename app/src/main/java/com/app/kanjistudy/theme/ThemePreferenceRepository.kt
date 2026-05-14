package com.app.kanjistudy.theme

import android.content.SharedPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class ThemePreferenceRepository @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {

    private val isDarkThemeFlow = MutableStateFlow(sharedPreferences.getBoolean(KEY_DARK_THEME, true))

    fun isDarkTheme(): Flow<Boolean> = isDarkThemeFlow.asStateFlow()

    fun setDarkTheme(enabled: Boolean) {
        sharedPreferences.edit { putBoolean(KEY_DARK_THEME, enabled) }
        isDarkThemeFlow.value = enabled
    }

    private companion object {
        const val KEY_DARK_THEME = "key_dark_theme"
    }
}