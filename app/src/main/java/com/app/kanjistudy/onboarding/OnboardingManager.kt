package com.app.kanjistudy.onboarding

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class OnboardingManager @Inject constructor(
    @ApplicationContext context: Context
) {
    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    fun shouldShowTutorial(): Boolean = !prefs.getBoolean(KEY_TUTORIAL_COMPLETED, false)

    fun setTutorialCompleted() {
        prefs.edit { putBoolean(KEY_TUTORIAL_COMPLETED, true) }
    }

    fun shouldShowHint(key: String): Boolean = !prefs.getBoolean("hint_$key", false)

    fun markHintShown(key: String) {
        prefs.edit { putBoolean("hint_$key", true) }
    }

    private companion object {
        const val PREFS_NAME = "onboarding_prefs"
        const val KEY_TUTORIAL_COMPLETED = "tutorial_completed"
    }
}