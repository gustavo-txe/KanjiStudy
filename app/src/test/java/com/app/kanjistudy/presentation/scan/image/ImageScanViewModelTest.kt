package com.app.kanjistudy.presentation.scan.image

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.app.kanjistudy.R
import com.app.kanjistudy.core.ui.UiText
import com.app.kanjistudy.data.image.GalleryImageLoader
import com.app.kanjistudy.data.ocr.ImageKanjiScanner
import com.app.kanjistudy.data.repository.KanjiRepository
import com.app.kanjistudy.testing.MainDispatcherRule
import com.app.kanjistudy.testing.kanji
import com.google.mlkit.vision.common.InputImage
import io.mockk.*
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ImageScanViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = mockk<KanjiRepository>()
    private val loader = mockk<GalleryImageLoader>()
    private val scan = mockk<ImageKanjiScanner>()
    private val uri = mockk<Uri>()
    private val image = mockk<InputImage>()
    private val store = ViewModelStore()
    private val catalog = MutableStateFlow(listOf(kanji(learned = true), kanji("月")) + List(2134) { kanji(it.toChar().toString()) })
    private fun viewModel(): ImageScanViewModel {
        coEvery { repository.isKanjiDownloadComplete() } returns true
        every { repository.observeKanjis() } returns catalog
        return ImageScanViewModel(loader, scan, repository, main.dispatcher).also { store.put("scan", it) }
    }
    @After fun tearDown() = store.clear()

    @Test fun `picker launches once until finished`() = runTest {
        val vm = viewModel()
        vm.uiEvent.test {
            vm.onScanImageClick()
            vm.onScanImageClick()
            assertTrue(vm.uiState.value.isSelectingImage)
            assertEquals(ImageKanjiScanUiEvent.LaunchDocumentScanner, awaitItem())
            expectNoEvents()
            vm.onImagePickerFinished()
            assertFalse(vm.uiState.value.isSelectingImage)
            vm.onScanImageClick()
            assertEquals(ImageKanjiScanUiEvent.LaunchDocumentScanner, awaitItem())
        }
    }

    @Test fun `scan exposes loading then recognized kanji with learned metadata`() = runTest {
        val result = CompletableDeferred<String>()
        coEvery { loader.load(uri) } returns image
        coEvery { scan(image) } coAnswers { result.await() }
        coEvery { repository.getKanjisByChars(setOf('日', '月')) } returns listOf(kanji(learned = true), kanji("月"))
        val vm = viewModel()
        vm.onImageSelected(uri)
        runCurrent()
        assertTrue(vm.uiState.value.isLoading)
        result.complete("日月日")
        runCurrent()
        val state = vm.uiState.value
        assertFalse(state.isLoading)
        assertEquals("日月日", state.recognizedKanji)
        assertEquals(setOf('日'), state.learnedKanjis)
        assertEquals(setOf('日', '月'), state.recognizedJoyoKanjis.keys)
        assertEquals(uri, state.selectedImageUri)
    }

    @Test fun `empty OCR result shows no kanji message`() = runTest {
        coEvery { loader.load(uri) } returns image
        coEvery { scan(image) } returns ""
        coEvery { repository.getKanjisByChars(emptySet()) } returns emptyList()
        val vm = viewModel()
        vm.onImageSelected(uri)
        runCurrent()
        assertEquals(UiText(R.string.scan_empty), vm.uiState.value.message)
        assertFalse(vm.uiState.value.isLoading)
    }

    @Test fun `image load failure shows recoverable error and retry scans the same URI`() = runTest {
        coEvery { loader.load(uri) } throws IOException("unreadable")
        val vm = viewModel()
        vm.onImageSelected(uri)
        runCurrent()
        assertEquals(UiText(R.string.scan_error), vm.uiState.value.message)
        assertFalse(vm.uiState.value.isLoading)
        coEvery { loader.load(uri) } returns image
        coEvery { scan(image) } returns "日"
        coEvery { repository.getKanjisByChars(setOf('日')) } returns listOf(kanji())
        vm.retryScan()
        runCurrent()
        assertEquals("日", vm.uiState.value.recognizedKanji)
    }

    @Test fun `removing image cancels pending scan and preserves download status`() = runTest {
        val result = CompletableDeferred<String>()
        coEvery { loader.load(uri) } returns image
        coEvery { scan(image) } coAnswers { result.await() }
        val vm = viewModel()
        vm.onImageSelected(uri)
        runCurrent()
        vm.removeImage()
        result.complete("日")
        runCurrent()
        assertEquals(ImageScanUiState(isKanjiDownloadComplete = true), vm.uiState.value)
        coVerify(exactly = 0) { repository.getKanjisByChars(any()) }
    }

    @Test fun `incomplete download blocks marking learned with an event`() = runTest {
        val vm = viewModel()
        coEvery { repository.isKanjiDownloadComplete() } returns false
        vm.uiEvent.test {
            vm.toggleLearnedKanji('日')
            assertEquals(ImageKanjiScanUiEvent.KanjiDownloadNotCompleted, awaitItem())
        }
        coVerify(exactly = 0) { repository.toggleLearnedKanji(any()) }
    }

    @Test fun `observation failure is reported and retry recovers metadata`() = runTest {
        coEvery { loader.load(uri) } returns image
        coEvery { scan(image) } returns "日"
        val vm = viewModel()
        var attempts = 0
        every { repository.observeKanjis() } returns flow {
            if (attempts++ == 0) throw IOException("database unavailable")
            emit(catalog.value)
        }
        vm.onImageSelected(uri)
        runCurrent()
        assertEquals(UiText(R.string.progress_load_error), vm.uiState.value.userMessage)
        testScheduler.advanceTimeBy(2_000)
        runCurrent()
        assertEquals(setOf('日'), vm.uiState.value.recognizedJoyoKanjis.keys)
        assertTrue(vm.uiState.value.isKanjiDownloadComplete)
    }

    @Test fun `new selection replaces a pending scan without showing old results`() = runTest {
        val oldResult = CompletableDeferred<String>()
        val secondUri = mockk<Uri>()
        val secondImage = mockk<InputImage>()
        coEvery { loader.load(uri) } returns image
        coEvery { scan(image) } coAnswers { oldResult.await() }
        coEvery { loader.load(secondUri) } returns secondImage
        coEvery { scan(secondImage) } returns "月"
        coEvery { repository.getKanjisByChars(setOf('月')) } returns listOf(kanji("月"))
        val vm = viewModel()
        vm.onImageSelected(uri)
        runCurrent()
        vm.onImageSelected(secondUri)
        runCurrent()
        oldResult.complete("日")
        runCurrent()
        assertEquals(secondUri, vm.uiState.value.selectedImageUri)
        assertEquals("月", vm.uiState.value.recognizedKanji)
        assertEquals(setOf('月'), vm.uiState.value.recognizedJoyoKanjis.keys)
    }

    @Test fun `learned kanji can be unmarked even while download is incomplete`() = runTest {
        coEvery { loader.load(uri) } returns image
        coEvery { scan(image) } returns "日"
        coEvery { repository.getKanjisByChars(setOf('日')) } returns listOf(kanji(learned = true))
        coEvery { repository.toggleLearnedKanji("日") } returns Unit
        val vm = viewModel()
        vm.onImageSelected(uri)
        runCurrent()
        coEvery { repository.isKanjiDownloadComplete() } returns false
        coEvery { repository.getKanjisByChars(setOf('日')) } returns listOf(kanji())
        vm.toggleLearnedKanji('日')
        catalog.value = catalog.value.map { it.copy(isLearned = false) }
        runCurrent()
        assertTrue(vm.uiState.value.learnedKanjis.isEmpty())
        coVerify(exactly = 1) { repository.toggleLearnedKanji("日") }
    }

    @Test fun `catalog updates cannot repopulate a removed image`() = runTest {
        coEvery { loader.load(uri) } returns image
        coEvery { scan(image) } returns "日"
        val vm = viewModel()
        vm.onImageSelected(uri)
        runCurrent()
        vm.removeImage()
        catalog.value = catalog.value.map { it.copy(isLearned = true) }
        runCurrent()
        assertEquals(ImageScanUiState(isKanjiDownloadComplete = true), vm.uiState.value)
    }

    @Test fun `catalog completion and external progress changes refresh an unchanged recognition`() = runTest {
        coEvery { loader.load(uri) } returns image
        coEvery { scan(image) } returns "日"
        val completeCatalog = catalog.value
        catalog.value = emptyList()
        val vm = viewModel()
        vm.onImageSelected(uri)
        runCurrent()
        assertTrue(vm.uiState.value.recognizedJoyoKanjis.isEmpty())
        catalog.value = completeCatalog
        runCurrent()
        assertEquals(setOf('日'), vm.uiState.value.learnedKanjis)
        assertTrue(vm.uiState.value.isKanjiDownloadComplete)
        catalog.value = completeCatalog.map { it.copy(isLearned = false) }
        runCurrent()
        assertTrue(vm.uiState.value.learnedKanjis.isEmpty())
        coVerify(exactly = 1) { scan(image) }
    }
}
