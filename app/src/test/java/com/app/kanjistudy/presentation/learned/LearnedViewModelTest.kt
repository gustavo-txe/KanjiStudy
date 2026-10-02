package com.app.kanjistudy.presentation.learned

import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.app.kanjistudy.data.repository.KanjiRepository
import com.app.kanjistudy.testing.MainDispatcherRule
import com.app.kanjistudy.testing.kanji
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LearnedViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun `learned changes preserve selected level and removing filter restores all entries`() = runTest {
        val repository = mockk<KanjiRepository>()
        val entries = MutableStateFlow(listOf(kanji(learned = true), kanji("月", 4, true)))
        every { repository.getLearnedKanjis() } returns entries
        coEvery { repository.toggleLearnedKanji("日") } returns Unit
        val store = ViewModelStore()
        val vm = LearnedViewModel(repository).also { store.put("learned", it) }
        try {
            vm.uiState.test {
                assertTrue(awaitItem().kanjis.isEmpty())
                assertEquals(entries.value, awaitItem().filteredKanjis)
                vm.onJlptLevelSelected(5)
                assertEquals(listOf(kanji(learned = true)), awaitItem().filteredKanjis)
                entries.value = listOf(kanji("月", 4, true))
                assertTrue(awaitItem().filteredKanjis.isEmpty())
                vm.onJlptLevelSelected(null)
                assertEquals(entries.value, awaitItem().filteredKanjis)
            }
            vm.toggleLearnedKanji("日")
            runCurrent()
            coVerify(exactly = 1) { repository.toggleLearnedKanji("日") }
        } finally { store.clear() }
    }
}
