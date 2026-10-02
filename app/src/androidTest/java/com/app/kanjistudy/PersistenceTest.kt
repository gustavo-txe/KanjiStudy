package com.app.kanjistudy

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import com.app.kanjistudy.data.local.AppDatabase
import com.app.kanjistudy.data.local.KanjiEntity
import com.app.kanjistudy.data.preferences.AppPreferencesRepository
import java.io.File
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith

@MediumTest
@RunWith(AndroidJUnit4::class)
class PersistenceTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    @get:Rule val temporary = TemporaryFolder(context.cacheDir)

    @Test fun learnedProgressSurvivesClosingAndReopeningDatabase() = runBlocking {
        withTimeout(10_000) {
            val path = File(temporary.root, "progress.db").path
            val sun = KanjiEntity("日", 5, listOf("ひ"), listOf("ニチ"), listOf("sun"))
            val database = Room.databaseBuilder(context, AppDatabase::class.java, path).build()
            try {
                database.kanjiDao().insertAll(listOf(sun))
                database.kanjiDao().toggleLearned("日")
            } finally {
                database.close()
            }

            val reopened = Room.databaseBuilder(context, AppDatabase::class.java, path).build()
            try {
                assertEquals(listOf(sun.copy(isLearned = true)), reopened.kanjiDao().getLearnedKanjisOnce())
            } finally {
                reopened.close()
            }
        }
    }

    @Test fun themeAndOnboardingSurviveCreatingANewDataStoreInstance() = runBlocking {
        withTimeout(10_000) {
            val file = File(temporary.root, "settings.preferences_pb")
            val firstJob = SupervisorJob()
            val original = AppPreferencesRepository(PreferenceDataStoreFactory.create(
                scope = CoroutineScope(firstJob + Dispatchers.IO),
                produceFile = { file },
            ))
            try {
                original.setDarkTheme(false)
                original.markHintShown("learned")
            } finally {
                firstJob.cancelAndJoin()
            }

            val secondJob = SupervisorJob()
            val reopened = AppPreferencesRepository(PreferenceDataStoreFactory.create(
                scope = CoroutineScope(secondJob + Dispatchers.IO),
                produceFile = { file },
            ))
            try {
                assertFalse(reopened.isDarkTheme.first())
                assertFalse(reopened.shouldShowHint("learned").first())
            } finally {
                secondJob.cancelAndJoin()
            }
        }
    }
}
