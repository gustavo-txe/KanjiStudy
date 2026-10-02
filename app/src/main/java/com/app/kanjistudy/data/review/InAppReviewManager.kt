package com.app.kanjistudy.data.review

import android.app.Activity
import android.util.Log
import com.app.kanjistudy.data.preferences.AppPreferencesRepository
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.ReviewManagerFactory
import java.io.IOException
import java.time.Instant
import java.time.ZoneOffset
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

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
        try {
            preferencesRepository.recordAppSessionStarted(today)
        } catch (exception: IOException) {
            Log.w("InAppReviewManager", "Unable to record session", exception)
        }
    }

    suspend fun maybeRequestReview(
        activity: Activity,
        sessionDurationMs: Long,
        nowMs: Long = System.currentTimeMillis()
    ) = withContext(Dispatchers.Main.immediate) {
        try {
            if (!canShowReviewPrompt(sessionDurationMs, nowMs)) return@withContext

            val reviewManager: ReviewManager = ReviewManagerFactory.create(activity)
            val reviewInfo = reviewManager.requestReviewFlow().awaitResultOrNull() ?: return@withContext
            val reviewLaunched = reviewManager.launchReviewFlow(activity, reviewInfo).awaitSuccessful()
            if (reviewLaunched) {
                markPromptShown(nowMs)
            }
        } catch (exception: IOException) {
            Log.w("InAppReviewManager", "Unable to persist review state", exception)
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

    private fun toEpochDay(timestampMs: Long): Long =
        Instant.ofEpochMilli(timestampMs).atZone(ZoneOffset.UTC).toLocalDate().toEpochDay()

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
