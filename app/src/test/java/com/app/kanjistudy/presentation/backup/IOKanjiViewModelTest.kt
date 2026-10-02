package com.app.kanjistudy.presentation.backup

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.app.kanjistudy.R
import com.app.kanjistudy.core.ui.UiText
import com.app.kanjistudy.data.document.JsonDocumentStore
import com.app.kanjistudy.data.repository.KanjiRepository
import com.app.kanjistudy.data.repository.LearnedKanjiImportResult
import com.app.kanjistudy.data.repository.KanjiCatalogIncompleteException
import com.app.kanjistudy.data.repository.ProgressRestorationPendingException
import com.app.kanjistudy.testing.MainDispatcherRule
import io.mockk.*
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class IOKanjiViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = mockk<KanjiRepository>()
    private val documents = mockk<JsonDocumentStore>()
    private val uri = mockk<Uri>()
    private val store = ViewModelStore()
    private fun viewModel() = IOKanjiViewModel(documents, repository).also { store.put("io", it) }
    @After fun tearDown() = store.clear()

    @Test fun `export writes repository JSON and exposes busy then success`() = runTest {
        coEvery { repository.exportLearnedKanjisJson() } returns "[\"日\"]"
        coEvery { documents.write(uri, any()) } returns Unit
        val vm = viewModel()
        vm.uiState.test {
            assertFalse(awaitItem().isBusy)
            vm.exportLearnedKanjis(uri)
            assertTrue(awaitItem().isBusy)
            val result = awaitItem()
            assertFalse(result.isBusy)
            assertNull(result.busyMessage)
            assertEquals(UiText(R.string.export_success), result.userMessage)
        }
        coVerify(exactly = 1) { documents.write(uri, "[\"日\"]") }
    }

    @Test fun `back to back actions cannot start a second operation`() = runTest {
        val pending = CompletableDeferred<String>()
        coEvery { repository.exportLearnedKanjisJson() } coAnswers { pending.await() }
        coEvery { documents.write(uri, any()) } returns Unit
        val vm = viewModel()
        vm.exportLearnedKanjis(uri)
        vm.exportLearnedKanjis(uri)
        vm.importLearnedKanjis(uri)
        runCurrent()
        assertTrue(vm.uiState.value.isBusy)
        coVerify(exactly = 1) { repository.exportLearnedKanjisJson() }
        coVerify(exactly = 0) { documents.read(any()) }
        pending.complete("[]")
        runCurrent()
        assertFalse(vm.uiState.value.isBusy)
    }

    @Test fun `import reports imported and skipped counts and message can be cleared`() = runTest {
        coEvery { documents.read(uri) } returns "[]"
        coEvery { repository.importLearnedKanjisJson("[]") } returns LearnedKanjiImportResult(2, 1)
        val vm = viewModel()
        vm.importLearnedKanjis(uri)
        runCurrent()
        assertEquals(UiText(R.string.import_success, listOf(2, 1)), vm.uiState.value.userMessage)
        assertFalse(vm.uiState.value.isBusy)
        vm.clearUserMessage()
        assertNull(vm.uiState.value.userMessage)
    }

    @Test fun `file error releases busy state and allows retry`() = runTest {
        coEvery { documents.read(uri) } throws IOException("Unable to open file")
        val vm = viewModel()
        vm.importLearnedKanjis(uri)
        runCurrent()
        assertFalse(vm.uiState.value.isBusy)
        assertEquals(UiText(R.string.import_error), vm.uiState.value.userMessage)
        coEvery { documents.read(uri) } returns "[]"
        coEvery { repository.importLearnedKanjisJson("[]") } returns LearnedKanjiImportResult(0, 0)
        vm.importLearnedKanjis(uri)
        assertNull(vm.uiState.value.userMessage)
        runCurrent()
        assertEquals(UiText(R.string.import_success, listOf(0, 0)), vm.uiState.value.userMessage)
    }

    @Test fun `clearing ViewModel cancels export without writing or showing an error`() = runTest {
        coEvery { repository.exportLearnedKanjisJson() } coAnswers { awaitCancellation() }
        val vm = viewModel()
        vm.exportLearnedKanjis(uri)
        runCurrent()
        store.clear()
        runCurrent()
        coVerify(exactly = 0) { documents.write(any(), any()) }
        assertNull(vm.uiState.value.userMessage)
    }

    @Test fun `export blocked by pending recovery preserves the selected document and explains the error`() = runTest {
        coEvery { repository.exportLearnedKanjisJson() } throws ProgressRestorationPendingException()
        val vm = viewModel()
        vm.exportLearnedKanjis(uri)
        runCurrent()
        assertFalse(vm.uiState.value.isBusy)
        assertEquals(UiText(R.string.progress_restore_pending), vm.uiState.value.userMessage)
        coVerify(exactly = 0) { documents.write(any(), any()) }
    }

    @Test fun `import blocked by an incomplete catalog explains how to complete the download`() = runTest {
        coEvery { documents.read(uri) } returns """["日"]"""
        coEvery { repository.importLearnedKanjisJson(any()) } throws KanjiCatalogIncompleteException()
        val vm = viewModel()
        vm.importLearnedKanjis(uri)
        runCurrent()
        assertFalse(vm.uiState.value.isBusy)
        assertEquals(UiText(R.string.progress_download_pending), vm.uiState.value.userMessage)
    }
}
