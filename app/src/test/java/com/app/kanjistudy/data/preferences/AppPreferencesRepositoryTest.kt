package com.app.kanjistudy.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import app.cash.turbine.test
import io.mockk.every
import io.mockk.mockk
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class AppPreferencesRepositoryTest {
    @get:Rule val temp = TemporaryFolder()

    private fun TestScope.repository(): AppPreferencesRepository {
        val file = File(temp.newFolder(), "test.preferences_pb")
        return AppPreferencesRepository(PreferenceDataStoreFactory.create(scope = backgroundScope, produceFile = { file }))
    }

    @Test fun `theme defaults to dark and emits saved changes`() = runTest {
        val repo = repository()
        repo.isDarkTheme.test {
            assertTrue(awaitItem())
            repo.setDarkTheme(false)
            assertFalse(awaitItem())
            repo.setDarkTheme(true)
            assertTrue(awaitItem())
        }
    }

    @Test fun `onboarding hints are independent and remembered`() = runTest {
        val repo = repository()
        assertTrue(repo.shouldShowHint("camera").first())
        repo.markHintShown("camera")
        assertFalse(repo.shouldShowHint("camera").first())
        assertTrue(repo.shouldShowHint("gallery").first())
    }

    @Test fun `schema refresh is required only for a newer version`() = runTest {
        val repo = repository()
        assertTrue(repo.shouldRefreshSchema(2))
        repo.markSchemaAsRefreshed(2)
        assertFalse(repo.shouldRefreshSchema(2))
        assertFalse(repo.shouldRefreshSchema(1))
        assertTrue(repo.shouldRefreshSchema(3))
    }

    @Test fun `sessions count consecutive days once and reset after a gap`() = runTest {
        val repo = repository()
        repo.recordAppSessionStarted(100)
        repo.recordAppSessionStarted(100)
        assertEquals(ReviewState(100, 1, 0, -1), repo.getReviewState(100))
        repo.recordAppSessionStarted(101)
        repo.recordAppSessionStarted(102)
        assertEquals(3L, repo.getReviewState(102).streakDays)
        repo.recordAppSessionStarted(104)
        assertEquals(ReviewState(100, 1, 0, -1), repo.getReviewState(104))
    }

    @Test fun `review prompts persist count and latest day`() = runTest {
        val repo = repository()
        repo.markReviewPromptShown(105)
        repo.markReviewPromptShown(120)
        assertEquals(ReviewState(120, 0, 2, 120), repo.getReviewState(120))
    }

    @Test fun `IO read errors use defaults but programming errors and cancellation propagate`() = runTest {
        val store = mockk<DataStore<Preferences>>()
        every { store.data } returns flow { throw IOException("unreadable") }
        assertTrue(AppPreferencesRepository(store).isDarkTheme.first())
        for (failure in listOf(IllegalStateException("bug"), CancellationException("cancelled"))) {
            every { store.data } returns flow { throw failure }
            try {
                AppPreferencesRepository(store).isDarkTheme.first()
                fail("Failure should propagate")
            } catch (actual: Exception) {
                assertSame(failure, actual)
            }
        }
    }
}
