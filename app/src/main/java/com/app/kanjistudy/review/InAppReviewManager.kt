package com.app.kanjistudy.review

import android.app.Activity
import android.os.Build
import com.app.kanjistudy.data.preferences.AppPreferencesRepository
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

@Singleton
class InAppReviewManager @Inject constructor(
    private val preferencesRepository: AppPreferencesRepository
) {
    private companion object {
        private const val MIN_DAYS_AFTER_INSTALL = 5L
        private const val MIN_STREAK_DAYS = 3L
        private const val MIN_SESSION_TIME_MS = 3 * 60 * 1000L
        private const val PROMPT_COOLDOWN_DAYS = 15L
        private const val MAX_PROMPTS = 4
    }

    suspend fun onAppSessionStarted(nowMs: Long = System.currentTimeMillis()) {
        val today = toEpochDay(nowMs)
        preferencesRepository.recordAppSessionStarted(today)
    }

    suspend fun maybeRequestReview(
        activity: Activity,
        sessionDurationMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.Main.immediate) {
        if (!canShowReviewPrompt(sessionDurationMs, nowMs)) return@withContext

        val reviewManager: ReviewManager = ReviewManagerFactory.create(activity)
        val reviewInfo = reviewManager.requestReviewFlow().awaitResultOrNull() ?: return@withContext
        val reviewLaunched = reviewManager.launchReviewFlow(activity, reviewInfo).awaitSuccessful()
        if (reviewLaunched) {
            markPromptShown(nowMs)
        }
    }

    private suspend fun canShowReviewPrompt(sessionDurationMs: Long, nowMs: Long): Boolean {
        if (sessionDurationMs < MIN_SESSION_TIME_MS) return false

        val today = toEpochDay(nowMs)
        val reviewState = preferencesRepository.getReviewState(today)
        if (today - reviewState.firstOpenDay < MIN_DAYS_AFTER_INSTALL) return false

        if (reviewState.streakDays < MIN_STREAK_DAYS) return false

        if (reviewState.promptCount >= MAX_PROMPTS) return false

        if (
            reviewState.lastPromptDay != -1L &&
            today - reviewState.lastPromptDay < PROMPT_COOLDOWN_DAYS
        ) {
            return false
        }

        return true
    }

    private suspend fun markPromptShown(nowMs: Long) {
        val today = toEpochDay(nowMs)
        preferencesRepository.markReviewPromptShown(today)
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

    private suspend fun <T> Task<T>.awaitResultOrNull(): T? {
        return suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener

                if (task.isSuccessful) {
                    continuation.resume(task.result)
                } else {
                    continuation.resume(null)
                }
            }
        }
    }

    private suspend fun Task<*>.awaitSuccessful(): Boolean {
        return suspendCancellableCoroutine { continuation ->
            addOnCompleteListener { task ->
                if (!continuation.isActive) return@addOnCompleteListener
                continuation.resume(task.isSuccessful)
            }
        }
    }
}
