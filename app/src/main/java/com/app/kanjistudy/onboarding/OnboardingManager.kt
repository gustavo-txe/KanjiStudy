package com.app.kanjistudy.onboarding

import com.app.kanjistudy.data.preferences.AppPreferencesRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class OnboardingManager @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository
) {
    fun shouldShowHint(key: String): Flow<Boolean> {
        return preferencesRepository.shouldShowHint(key)
    }

    suspend fun markHintShown(key: String) {
        preferencesRepository.markHintShown(key)
    }
}
