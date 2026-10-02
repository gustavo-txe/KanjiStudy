package com.app.kanjistudy.presentation.backup

import com.app.kanjistudy.R
import com.app.kanjistudy.core.ui.UiText
import com.app.kanjistudy.data.repository.KanjiRepository
import com.app.kanjistudy.data.repository.ProgressRestorationPendingException
import com.app.kanjistudy.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutoBackupViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun `retry prevents parallel attempts and reports restoration error without changing progress`() = runTest {
        val repository = mockk<KanjiRepository>()
        every { repository.isAutoBackupPending } returns MutableStateFlow(true)
        val finished = CompletableDeferred<Unit>()
        coEvery { repository.retryAutoBackup() } coAnswers {
            finished.await()
            throw ProgressRestorationPendingException()
        }
        val viewModel = AutoBackupViewModel(repository)
        viewModel.retry()
        viewModel.retry()
        runCurrent()
        assertTrue(viewModel.isRetrying.value)
        finished.complete(Unit)
        runCurrent()
        assertFalse(viewModel.isRetrying.value)
        assertEquals(UiText(R.string.progress_restore_pending), viewModel.error.value)
        coVerify(exactly = 1) { repository.retryAutoBackup() }
        coVerify(exactly = 0) { repository.toggleLearnedKanji(any()) }
    }
}
