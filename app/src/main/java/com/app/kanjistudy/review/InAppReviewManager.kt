package com.app.kanjistudy.review

import android.app.Activity
import android.content.SharedPreferences
import android.os.Build
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class InAppReviewManager @Inject constructor(
    private val sharedPreferences: SharedPreferences
) {
    private companion object {
        private const val KEY_FIRST_OPEN_DAY = "first_open_day"
        private const val KEY_LAST_OPEN_DAY = "last_open_day"
        private const val KEY_STREAK_DAYS = "streak_days"
        private const val KEY_LAST_PROMPT_DAY = "last_prompt_day"
        private const val KEY_PROMPT_COUNT = "prompt_count"

        private const val MIN_DAYS_AFTER_INSTALL = 0L
        private const val MIN_STREAK_DAYS = 0L
        private const val MIN_SESSION_TIME_MS = 10 * 1000L
        private const val PROMPT_COOLDOWN_DAYS = 0L
        private const val MAX_PROMPTS = 50
    }

    fun onAppSessionStarted(nowMs: Long = System.currentTimeMillis()) {
        val today = toEpochDay(nowMs)
        val firstOpenDay = sharedPreferences.getLong(KEY_FIRST_OPEN_DAY, -1L)
        if (firstOpenDay == -1L) {
            sharedPreferences.edit { putLong(KEY_FIRST_OPEN_DAY, today) }
        }

        val lastOpenDay = sharedPreferences.getLong(KEY_LAST_OPEN_DAY, -1L)
        if (lastOpenDay != today) {
            val currentStreak = sharedPreferences.getLong(KEY_STREAK_DAYS, 0L)
            val updatedStreak = when {
                lastOpenDay == -1L -> 1L
                today == lastOpenDay + 1L -> currentStreak + 1L
                else -> 1L
            }
            sharedPreferences.edit {
                putLong(KEY_STREAK_DAYS, updatedStreak)
                    .putLong(KEY_LAST_OPEN_DAY, today)
            }
        }
    }

    fun maybeRequestReview(activity: Activity, sessionDurationMs: Long, nowMs: Long = System.currentTimeMillis()) {
        if (!canShowReviewPrompt(sessionDurationMs, nowMs)) return

        val reviewManager: ReviewManager = ReviewManagerFactory.create(activity)
        reviewManager.requestReviewFlow().addOnCompleteListener { requestTask ->
            if (requestTask.isSuccessful) {
                val reviewInfo = requestTask.result
                reviewManager.launchReviewFlow(activity, reviewInfo).addOnCompleteListener {
                    markPromptShown(nowMs)
                }
            }
        }
    }

    private fun canShowReviewPrompt(sessionDurationMs: Long, nowMs: Long): Boolean {
        if (sessionDurationMs < MIN_SESSION_TIME_MS) return false

        val today = toEpochDay(nowMs)
        val firstOpenDay = sharedPreferences.getLong(KEY_FIRST_OPEN_DAY, today)
        if (today - firstOpenDay < MIN_DAYS_AFTER_INSTALL) return false

        val streakDays = sharedPreferences.getLong(KEY_STREAK_DAYS, 0L)
        if (streakDays < MIN_STREAK_DAYS) return false

        val promptCount = sharedPreferences.getInt(KEY_PROMPT_COUNT, 0)
        if (promptCount >= MAX_PROMPTS) return false

        val lastPromptDay = sharedPreferences.getLong(KEY_LAST_PROMPT_DAY, -1L)
        if (lastPromptDay != -1L && today - lastPromptDay < PROMPT_COOLDOWN_DAYS) return false

        return true
    }

    private fun markPromptShown(nowMs: Long) {
        val today = toEpochDay(nowMs)
        val promptCount = sharedPreferences.getInt(KEY_PROMPT_COUNT, 0)
        sharedPreferences.edit {
            putInt(KEY_PROMPT_COUNT, promptCount + 1)
                .putLong(KEY_LAST_PROMPT_DAY, today)
        }
    }

    private fun toEpochDay(timestampMs: Long): Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Instant.ofEpochMilli(timestampMs)
                .atZone(ZoneOffset.UTC)
                .toLocalDate()
                .toEpochDay()
        } else {
            TODO("VERSION.SDK_INT < O")
        }
    }
}