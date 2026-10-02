package com.app.kanjistudy.presentation

import androidx.lifecycle.ViewModelStore
import com.app.kanjistudy.R
import com.app.kanjistudy.core.time.MonotonicClock
import com.app.kanjistudy.core.ui.UiText
import com.app.kanjistudy.data.image.GalleryImageLoader
import com.app.kanjistudy.data.ocr.CameraKanjiScanner
import com.app.kanjistudy.data.ocr.ImageKanjiScanner
import com.app.kanjistudy.data.preferences.ThemePreferenceRepository
import com.app.kanjistudy.data.repository.KanjiRepository
import com.app.kanjistudy.presentation.home.KanjiViewModel
import com.app.kanjistudy.presentation.learned.LearnedViewModel
import com.app.kanjistudy.presentation.scan.camera.CameraScanViewModel
import com.app.kanjistudy.presentation.scan.image.ImageScanViewModel
import com.app.kanjistudy.presentation.theme.ThemeViewModel
import com.app.kanjistudy.testing.MainDispatcherRule
import com.app.kanjistudy.testing.kanji
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PersistenceFailureTest {
    @get:Rule val main = MainDispatcherRule()
    private val store = ViewModelStore()
    private val repository = mockk<KanjiRepository> {
        every { getLearnedKanjis() } returns flowOf(emptyList())
        every { observeKanjis() } returns flowOf(emptyList())
        coEvery { ensureAllKanjisLoaded(any()) } returns listOf(kanji())
        coEvery { isKanjiDownloadComplete() } returns true
        coEvery { toggleLearnedKanji(any()) } throws IOException("storage unavailable")
    }

    @After fun tearDown() = store.clear()

    @Test fun `home reports failed writes and allows the same action to be retried`() = runTest {
        val vm = KanjiViewModel(repository, main.dispatcher).also { store.put("home", it) }
        vm.markLearnedKanji("日")
        runCurrent()
        assertEquals(UiText(R.string.progress_save_error), vm.uiState.value.userMessage)
        assertEquals(listOf("日"), vm.uiState.value.filteredKanjis)
        vm.clearUserMessage()
        coEvery { repository.toggleLearnedKanji("日") } returns Unit
        vm.markLearnedKanji("日")
        runCurrent()
        assertNull(vm.uiState.value.userMessage)
        coVerify(exactly = 2) { repository.toggleLearnedKanji("日") }
    }

    @Test fun `cancelling a write does not produce an error message`() = runTest {
        coEvery { repository.toggleLearnedKanji(any()) } throws CancellationException()
        val vm = KanjiViewModel(repository, main.dispatcher).also { store.put("home", it) }
        vm.markLearnedKanji("日")
        runCurrent()
        assertNull(vm.uiState.value.userMessage)
    }

    @Test fun `learned observation can be restarted after failure and write errors preserve data`() = runTest {
        every { repository.getLearnedKanjis() } returns flow { throw IOException("storage unavailable") }
        val vm = LearnedViewModel(repository).also { store.put("learned", it) }
        runCurrent()
        assertEquals(UiText(R.string.progress_load_error), vm.uiState.value.error)
        val learned = listOf(kanji(learned = true))
        every { repository.getLearnedKanjis() } returns flowOf(learned)
        vm.observeLearnedKanjis()
        runCurrent()
        assertNull(vm.uiState.value.error)
        assertEquals(learned, vm.uiState.value.kanjis)
        vm.toggleLearnedKanji("日")
        runCurrent()
        assertEquals(UiText(R.string.progress_save_error), vm.uiState.value.userMessage)
        assertEquals(learned, vm.uiState.value.kanjis)
    }

    @Test fun `both scanners report failed progress writes`() = runTest {
        val camera = CameraScanViewModel(mockk<CameraKanjiScanner>(), repository, main.dispatcher, mockk<MonotonicClock>())
            .also { store.put("camera", it) }
        val image = ImageScanViewModel(mockk<GalleryImageLoader>(), mockk<ImageKanjiScanner>(), repository, main.dispatcher)
            .also { store.put("image", it) }
        camera.toggleLearnedKanji('日')
        image.toggleLearnedKanji('日')
        runCurrent()
        assertEquals(UiText(R.string.progress_save_error), camera.uiState.value.userMessage)
        assertEquals(UiText(R.string.progress_save_error), image.uiState.value.userMessage)
    }

    @Test fun `theme write failure is reported and can be retried`() = runTest {
        val preferences = mockk<ThemePreferenceRepository> {
            every { isDarkTheme() } returns flowOf(true)
            coEvery { setDarkTheme(false) } throws IOException("disk full")
        }
        val vm = ThemeViewModel(preferences).also { store.put("theme", it) }
        vm.onThemeChanged(false)
        runCurrent()
        assertEquals(UiText(R.string.preferences_save_error), vm.userMessage.value)
        vm.clearUserMessage()
        coEvery { preferences.setDarkTheme(false) } returns Unit
        vm.onThemeChanged(false)
        runCurrent()
        assertNull(vm.userMessage.value)
        coVerify(exactly = 2) { preferences.setDarkTheme(false) }
    }
}
