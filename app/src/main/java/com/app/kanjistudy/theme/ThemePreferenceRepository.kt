package com.app.kanjistudy.theme

import com.app.kanjistudy.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ThemePreferenceRepository @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository
) {

    fun isDarkTheme(): Flow<Boolean> = preferencesRepository.isDarkTheme

    suspend fun setDarkTheme(enabled: Boolean) {
        preferencesRepository.setDarkTheme(enabled)
    }
}
