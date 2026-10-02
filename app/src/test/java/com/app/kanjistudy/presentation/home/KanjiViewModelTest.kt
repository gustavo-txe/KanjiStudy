package com.app.kanjistudy.presentation.home

import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.app.kanjistudy.R
import com.app.kanjistudy.core.ui.UiText
import com.app.kanjistudy.data.repository.KanjiRepository
import com.app.kanjistudy.testing.MainDispatcherRule
import com.app.kanjistudy.testing.kanji
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KanjiViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = mockk<KanjiRepository>()
    private val learned = MutableStateFlow(listOf(kanji(learned = true)))
    private val store = ViewModelStore()
    private val data = listOf(
        kanji(),
        kanji("月", 4).copy(meanings = listOf("moon; month")),
        kanji("明", null).copy(meanings = listOf("bright sunlight")),
    )

    private fun viewModel(): KanjiViewModel {
        every { repository.getLearnedKanjis() } returns learned
        return KanjiViewModel(repository, main.dispatcher).also { store.put("kanji", it) }
    }

    @After fun tearDown() = store.clear()

    @Test fun `loading exposes progress and then downloaded data`() = runTest {
        val download = CompletableDeferred<List<com.app.kanjistudy.domain.model.Kanji>>()
        coEvery { repository.ensureAllKanjisLoaded(any()) } coAnswers {
            firstArg<(Float) -> Unit>()(0.5f)
            download.await()
        }
        val vm = viewModel()
        vm.uiState.test {
            assertTrue(awaitItem().isLoading)
            assertEquals(0.5f, awaitItem().loadingProgress)
            download.complete(data)
            runCurrent()
            val state = expectMostRecentItem()
            assertFalse(state.isLoading)
            assertTrue(state.isCatalogReady)
            assertEquals(listOf("日", "月", "明"), state.filteredKanjis)
            assertEquals(listOf("ひ"), state.kunReadings["日"])
            assertEquals(1f, state.loadingProgress)
            assertNull(state.error)
        }
    }

    @Test fun `download failure stops loading and shows actionable error`() = runTest {
        coEvery { repository.ensureAllKanjisLoaded(any()) } throws IOException("offline")
        val vm = viewModel()
        runCurrent()
        assertFalse(vm.uiState.value.isLoading)
        assertFalse(vm.uiState.value.isCatalogReady)
        assertEquals(UiText(R.string.catalog_load_error), vm.uiState.value.error)
        assertTrue(vm.uiState.value.joyoKanjis.isEmpty())
    }

    @Test fun `retry clears error recovers catalog and ignores duplicate requests`() = runTest {
        coEvery { repository.ensureAllKanjisLoaded(any()) } throws IOException("offline")
        val vm = viewModel()
        runCurrent()
        val download = CompletableDeferred<List<com.app.kanjistudy.domain.model.Kanji>>()
        coEvery { repository.ensureAllKanjisLoaded(any()) } coAnswers { download.await() }
        vm.retryLoading()
        vm.retryLoading()
        runCurrent()
        assertTrue(vm.uiState.value.isLoading)
        assertNull(vm.uiState.value.error)
        assertEquals(0f, vm.uiState.value.loadingProgress)
        download.complete(data)
        runCurrent()
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.isCatalogReady)
        assertEquals(3, vm.uiState.value.filteredKanjis.size)
        coVerify(exactly = 2) { repository.ensureAllKanjisLoaded(any()) }
    }

    @Test fun `typing waits for submission and matches whole terms ignoring case and whitespace`() = runTest {
        coEvery { repository.ensureAllKanjisLoaded(any()) } returns data
        val vm = viewModel()
        runCurrent()
        vm.onQueryChange("  SUN  ")
        assertEquals(3, vm.uiState.value.filteredKanjis.size)
        vm.onSearchRequested()
        runCurrent()
        assertEquals(listOf("日"), vm.uiState.value.filteredKanjis)
        vm.onQueryChange("month")
        vm.onSearchRequested()
        runCurrent()
        assertEquals(listOf("月"), vm.uiState.value.filteredKanjis)
    }

    @Test fun `search supports characters readings and empty query`() = runTest {
        coEvery { repository.ensureAllKanjisLoaded(any()) } returns listOf(kanji())
        val vm = viewModel()
        runCurrent()
        for (query in listOf("日", "ひ", "ニチ", " ")) {
            vm.onQueryChange(query)
            vm.onSearchRequested()
            runCurrent()
            assertEquals(query, listOf("日"), vm.uiState.value.filteredKanjis)
        }
        vm.onQueryChange("missing")
        vm.onSearchRequested()
        runCurrent()
        assertTrue(vm.uiState.value.filteredKanjis.isEmpty())
    }

    @Test fun `level filter combines with submitted query and can be cleared`() = runTest {
        coEvery { repository.ensureAllKanjisLoaded(any()) } returns data
        val vm = viewModel()
        runCurrent()
        vm.onQueryChange("sun")
        vm.onSearchRequested()
        runCurrent()
        vm.onJlptLevelSelected(4)
        runCurrent()
        assertTrue(vm.uiState.value.filteredKanjis.isEmpty())
        vm.onJlptLevelSelected(null)
        runCurrent()
        assertEquals(listOf("日"), vm.uiState.value.filteredKanjis)
    }

    @Test fun `dialog events keep the selected kanji and learned status`() = runTest {
        coEvery { repository.ensureAllKanjisLoaded(any()) } returns data
        val vm = viewModel()
        vm.uiEvent.test {
            vm.onKanjiSelected("月")
            assertEquals(KanjiUiEvent.ShowDialog(KanjiDialog.Actions("月")), awaitItem())
            vm.addLearnedKanji("日", true)
            assertEquals(KanjiUiEvent.ShowDialog(KanjiDialog.ToggleLearned("日", true)), awaitItem())
        }
    }

    @Test fun `learned flow follows repository while subscribed and marking delegates once`() = runTest {
        coEvery { repository.ensureAllKanjisLoaded(any()) } returns data
        coEvery { repository.toggleLearnedKanji("日") } returns Unit
        val vm = viewModel()
        vm.learnedKanjis.test {
            assertEquals(emptyList<Any>(), awaitItem())
            assertEquals(learned.value, awaitItem())
            learned.value = emptyList()
            assertEquals(emptyList<Any>(), awaitItem())
        }
        vm.markLearnedKanji("日")
        runCurrent()
        coVerify(exactly = 1) { repository.toggleLearnedKanji("日") }
    }

    @Test fun `rapid search submissions publish only the latest query result`() = runTest {
        coEvery { repository.ensureAllKanjisLoaded(any()) } returns data
        val vm = viewModel()
        runCurrent()
        vm.onQueryChange("sun")
        vm.onSearchRequested()
        vm.onQueryChange("moon")
        vm.onSearchRequested()
        runCurrent()
        assertEquals("moon", vm.uiState.value.submittedQuery)
        assertEquals(listOf("月"), vm.uiState.value.filteredKanjis)
    }
    @Test fun `a level change immediately after search keeps the submitted query`() = runTest {
        coEvery { repository.ensureAllKanjisLoaded(any()) } returns data
        val vm = viewModel()
        runCurrent()
        vm.onQueryChange("sun")
        vm.onSearchRequested()
        vm.onJlptLevelSelected(5)
        runCurrent()
        assertEquals("sun", vm.uiState.value.submittedQuery)
        assertEquals(5, vm.uiState.value.selectedJlptLevel)
        assertEquals(listOf("日"), vm.uiState.value.filteredKanjis)
    }
}
