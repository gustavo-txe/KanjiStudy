package com.app.kanjistudy.data.preferences

import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow

@Singleton
class ThemePreferenceRepository @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository
) {

    fun isDarkTheme(): Flow<Boolean> = preferencesRepository.isDarkTheme

    suspend fun setDarkTheme(enabled: Boolean) {
        preferencesRepository.setDarkTheme(enabled)
    }
}
