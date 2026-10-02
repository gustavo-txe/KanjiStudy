package com.app.kanjistudy.presentation.scan.camera

import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.app.kanjistudy.core.time.MonotonicClock
import com.app.kanjistudy.data.ocr.CameraKanjiScanner
import com.app.kanjistudy.data.repository.KanjiRepository
import com.app.kanjistudy.testing.MainDispatcherRule
import com.app.kanjistudy.testing.kanji
import com.google.mlkit.vision.common.InputImage
import io.mockk.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import org.junit.After
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CameraScanViewModelTest {
    @get:Rule val main = MainDispatcherRule()
    private val repository = mockk<KanjiRepository>()
    private val clock = mockk<MonotonicClock> { every { nowMillis() } returns 1_000L }
    private val scan = mockk<CameraKanjiScanner>()
    private val store = ViewModelStore()
    private val catalog = MutableStateFlow(List(2136) { kanji(it.toChar().toString()) })
    private fun viewModel(complete: Boolean = true): CameraScanViewModel {
        coEvery { repository.isKanjiDownloadComplete() } returns complete
        if (!complete) catalog.value = emptyList()
        every { repository.observeKanjis() } returns catalog
        return CameraScanViewModel(scan, repository, main.dispatcher, clock).also { store.put("camera", it) }
    }
    @After fun tearDown() = store.clear()

    @Test fun `paused camera closes frame without accessing image or invoking OCR`() = runTest {
        val frame = mockk<ImageProxy>()
        every { frame.close() } just Runs
        val vm = viewModel()
        vm.togglePause()
        vm.onFrame(frame)
        assertTrue(vm.uiState.value.isPaused)
        verify(exactly = 1) { frame.close() }
        verify(exactly = 0) { frame.image }
        coVerify(exactly = 0) { scan.analyze(any(), any()) }
        vm.togglePause()
        assertFalse(vm.uiState.value.isPaused)
    }

    @Test fun `frame without image is closed`() = runTest {
        val frame = mockk<ImageProxy>()
        every { frame.close() } just Runs
        every { frame.image } returns null
        viewModel().onFrame(frame)
        verify(exactly = 1) { frame.close() }
        coVerify(exactly = 0) { scan.analyze(any(), any()) }
    }

    @Test fun `clearing view model before queued analysis closes accepted frame`() = runTest {
        val frame = mockk<ImageProxy>()
        every { frame.close() } just Runs
        every { frame.image } returns mockk<android.media.Image>()
        val vm = viewModel()
        vm.onFrame(frame)
        store.clear()
        runCurrent()
        verify(exactly = 1) { frame.close() }
        verify(exactly = 0) { frame.imageInfo }
        coVerify(exactly = 0) { scan.analyze(any(), any()) }
    }

    @Test fun `frame stays open until in flight OCR finishes after view model is cleared`() = runTest {
        mockkStatic(InputImage::class)
        val finished = CompletableDeferred<Unit>()
        try {
            val mediaImage = mockk<android.media.Image>()
            val input = mockk<InputImage>()
            val frame = mockk<ImageProxy>()
            every { frame.close() } just Runs
            every { frame.image } returns mediaImage
            every { frame.imageInfo.rotationDegrees } returns 0
            every { InputImage.fromMediaImage(mediaImage, 0) } returns input
            coEvery { scan.analyze(input, any()) } coAnswers {
                withContext(NonCancellable) { finished.await() }
                null
            }
            val vm = viewModel()
            vm.onFrame(frame)
            runCurrent()
            coVerify(exactly = 1) { scan.analyze(input, any()) }
            store.clear()
            runCurrent()
            verify(exactly = 0) { frame.close() }
            finished.complete(Unit)
            runCurrent()
            verify(exactly = 1) { frame.close() }
        } finally {
            finished.complete(Unit)
            unmockkStatic(InputImage::class)
        }
    }

    @Test fun `failure creating input image still closes the frame`() = runTest {
        mockkStatic(InputImage::class)
        try {
            val mediaImage = mockk<android.media.Image>()
            val frame = mockk<ImageProxy>()
            every { frame.close() } just Runs
            every { frame.image } returns mediaImage
            every { frame.imageInfo.rotationDegrees } returns 0
            every { InputImage.fromMediaImage(mediaImage, 0) } throws IllegalArgumentException("invalid image")
            viewModel().onFrame(frame)
            runCurrent()
            verify(exactly = 1) { frame.close() }
        } finally {
            unmockkStatic(InputImage::class)
        }
    }

    @Test fun `long press emits copy event`() = runTest {
        val vm = viewModel()
        vm.uiEvent.test {
            vm.onKanjiLongClick('日')
            assertEquals(ScanUiEvent.CopyKanji('日'), awaitItem())
        }
    }

    @Test fun `incomplete download prevents marking learned`() = runTest {
        val vm = viewModel(complete = false)
        vm.uiEvent.test {
            vm.toggleLearnedKanji('日')
            assertEquals(ScanUiEvent.KanjiDownloadNotCompleted, awaitItem())
        }
        assertFalse(vm.uiState.value.isKanjiDownloadComplete)
        coVerify(exactly = 0) { repository.toggleLearnedKanji(any()) }
    }

    @Test fun `complete download allows marking learned and refreshes status`() = runTest {
        coEvery { repository.toggleLearnedKanji("日") } returns Unit
        coEvery { repository.getKanjisByChars(emptySet()) } returns emptyList()
        val vm = viewModel()
        vm.toggleLearnedKanji('日')
        runCurrent()
        assertTrue(vm.uiState.value.isKanjiDownloadComplete)
        coVerify(exactly = 1) { repository.toggleLearnedKanji("日") }
        coEvery { repository.isKanjiDownloadComplete() } returns false
        catalog.value = emptyList()
        runCurrent()
        assertFalse(vm.uiState.value.isKanjiDownloadComplete)
    }
    @Test fun `catalog and progress changes update an unchanged camera result`() = runTest {
        mockkStatic(InputImage::class)
        try {
            val mediaImage = mockk<android.media.Image>()
            val input = mockk<InputImage>()
            val frame = mockk<ImageProxy>()
            every { frame.close() } just Runs
            every { frame.image } returns mediaImage
            every { frame.imageInfo.rotationDegrees } returns 0
            every { InputImage.fromMediaImage(mediaImage, 0) } returns input
            coEvery { scan.analyze(input, any()) } returns "日"
            val vm = viewModel(complete = false)
            vm.onFrame(frame)
            runCurrent()
            assertTrue(vm.uiState.value.recognizedJoyoKanjis.isEmpty())
            catalog.value = listOf(kanji(learned = true)) + List(2135) { kanji(it.toChar().toString()) }
            runCurrent()
            assertEquals(setOf('日'), vm.uiState.value.learnedKanjis)
            assertTrue(vm.uiState.value.isKanjiDownloadComplete)
            catalog.value = catalog.value.map { it.copy(isLearned = false) }
            runCurrent()
            assertTrue(vm.uiState.value.learnedKanjis.isEmpty())
            coVerify(exactly = 1) { scan.analyze(input, any()) }
        } finally {
            unmockkStatic(InputImage::class)
        }
    }
}
