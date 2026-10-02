package com.app.kanjistudy.data.preferences

import android.util.Log
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch

@Singleton
class OnboardingManager @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository
) {
    fun shouldShowHint(key: String): Flow<Boolean> {
        return preferencesRepository.shouldShowHint(key).catch { exception ->
            if (exception !is IOException) throw exception
            emit(false)
        }
    }

    suspend fun markHintShown(key: String) {
        try {
            preferencesRepository.markHintShown(key)
        } catch (exception: IOException) {
            Log.w("OnboardingManager", "Unable to persist hint state", exception)
        }
    }
}
