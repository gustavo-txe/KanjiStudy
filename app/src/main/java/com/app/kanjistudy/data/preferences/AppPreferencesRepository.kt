package com.app.kanjistudy.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppPreferencesRepository @Inject constructor(
    private val dataStore: DataStore<Preferences>
) {
    val isDarkTheme: Flow<Boolean> = preferencesFlow
        .map { preferences -> preferences[KEY_DARK_THEME] ?: true }

    fun shouldShowHint(key: String): Flow<Boolean> = preferencesFlow
        .map { preferences -> preferences[hintKey(key)] != true }

    suspend fun setDarkTheme(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[KEY_DARK_THEME] = enabled
        }
    }

    suspend fun markHintShown(key: String) {
        dataStore.edit { preferences ->
            preferences[hintKey(key)] = true
        }
    }

    suspend fun shouldRefreshSchema(databaseVersion: Int): Boolean {
        return preferencesFlow
            .map { preferences -> preferences[KEY_LAST_REFRESHED_SCHEMA_VERSION] ?: 0 }
            .first() < databaseVersion
    }

    suspend fun markSchemaAsRefreshed(databaseVersion: Int) {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_REFRESHED_SCHEMA_VERSION] = databaseVersion
        }
    }

    suspend fun recordAppSessionStarted(today: Long) {
        dataStore.edit { preferences ->
            if (preferences[KEY_FIRST_OPEN_DAY] == null) {
                preferences[KEY_FIRST_OPEN_DAY] = today
            }

            val lastOpenDay = preferences[KEY_LAST_OPEN_DAY] ?: -1L
            if (lastOpenDay != today) {
                val currentStreak = preferences[KEY_STREAK_DAYS] ?: 0L
                preferences[KEY_STREAK_DAYS] = when {
                    lastOpenDay == -1L -> 1L
                    today == lastOpenDay + 1L -> currentStreak + 1L
                    else -> 1L
                }
                preferences[KEY_LAST_OPEN_DAY] = today
            }
        }
    }

    suspend fun getReviewState(today: Long): ReviewState {
        return preferencesFlow
            .map { preferences ->
                ReviewState(
                    firstOpenDay = preferences[KEY_FIRST_OPEN_DAY] ?: today,
                    streakDays = preferences[KEY_STREAK_DAYS] ?: 0L,
                    promptCount = preferences[KEY_PROMPT_COUNT] ?: 0,
                    lastPromptDay = preferences[KEY_LAST_PROMPT_DAY] ?: -1L
                )
            }
            .first()
    }

    suspend fun markReviewPromptShown(today: Long) {
        dataStore.edit { preferences ->
            val promptCount = preferences[KEY_PROMPT_COUNT] ?: 0
            preferences[KEY_PROMPT_COUNT] = promptCount + 1
            preferences[KEY_LAST_PROMPT_DAY] = today
        }
    }

    private val preferencesFlow: Flow<Preferences>
        get() = dataStore.data.catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    private fun hintKey(key: String): Preferences.Key<Boolean> {
        return booleanPreferencesKey("hint_$key")
    }

    private companion object {
        val KEY_DARK_THEME = booleanPreferencesKey("key_dark_theme")
        val KEY_LAST_REFRESHED_SCHEMA_VERSION = intPreferencesKey("last_refreshed_schema_version")
        val KEY_FIRST_OPEN_DAY = longPreferencesKey("first_open_day")
        val KEY_LAST_OPEN_DAY = longPreferencesKey("last_open_day")
        val KEY_STREAK_DAYS = longPreferencesKey("streak_days")
        val KEY_LAST_PROMPT_DAY = longPreferencesKey("last_prompt_day")
        val KEY_PROMPT_COUNT = intPreferencesKey("prompt_count")
    }
}

data class ReviewState(
    val firstOpenDay: Long,
    val streakDays: Long,
    val promptCount: Int,
    val lastPromptDay: Long
)
