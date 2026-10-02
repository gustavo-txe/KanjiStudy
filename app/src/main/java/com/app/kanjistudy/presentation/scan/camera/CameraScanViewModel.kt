package com.app.kanjistudy.presentation.scan.camera

import android.annotation.SuppressLint
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.R
import com.app.kanjistudy.core.navigation.googleAISearch
import com.app.kanjistudy.core.time.MonotonicClock
import com.app.kanjistudy.core.ui.UiText
import com.app.kanjistudy.core.ui.toProgressErrorMessage
import com.app.kanjistudy.data.ocr.CameraKanjiScanner
import com.app.kanjistudy.data.repository.KanjiRepository
import com.app.kanjistudy.di.DefaultDispatcher
import com.app.kanjistudy.domain.model.Kanji
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class CameraScanViewModel @Inject constructor(
    private val scanner: CameraKanjiScanner,
    private val kanjiRepository: KanjiRepository,
    @DefaultDispatcher dispatcher: CoroutineDispatcher,
    private val clock: MonotonicClock
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraScanUiState())
    val uiState: StateFlow<CameraScanUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ScanUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val isProcessingFrame = AtomicBoolean(false)

    private val lastFrameAcceptedAtMs = AtomicLong(-FRAME_ANALYSIS_INTERVAL_MS)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val analysisDispatcher = dispatcher.limitedParallelism(1)

    private companion object {
        const val FRAME_ANALYSIS_INTERVAL_MS = 350L
    }

    private var catalog: List<Kanji> = emptyList()

    init {
        viewModelScope.launch(analysisDispatcher) {
            kanjiRepository.observeKanjis()
                .retryWhen { cause, _ ->
                    if (cause !is Exception || cause is CancellationException) throw cause
                    _uiState.update { it.copy(userMessage = UiText(R.string.progress_load_error)) }
                    delay(2_000)
                    true
                }
                .collect { kanjis ->
                    catalog = kanjis
                    _uiState.update { state ->
                        withMetadata(state).copy(isKanjiDownloadComplete = kanjis.size >= 2136)
                    }
                }
        }
    }

    fun onCameraError() {
        _uiState.update { it.copy(userMessage = UiText(R.string.camera_unavailable)) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun onFrame(imageProxy: ImageProxy) {
        if (_uiState.value.isPaused) {
            imageProxy.close()
            return
        }
        val now = clock.nowMillis()
        val lastAccepted = lastFrameAcceptedAtMs.get()
        if (now - lastAccepted < FRAME_ANALYSIS_INTERVAL_MS) {
            imageProxy.close()
            return
        }
        if (!lastFrameAcceptedAtMs.compareAndSet(lastAccepted, now)) {
            imageProxy.close()
            return
        }
        if (!isProcessingFrame.compareAndSet(false, true)) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image ?: run {
            isProcessingFrame.set(false)
            imageProxy.close()
            return
        }

        viewModelScope.launch(analysisDispatcher) {
            try {
                val image = InputImage.fromMediaImage(
                    mediaImage,
                    imageProxy.imageInfo.rotationDegrees
                )
                val newKanji = scanner.analyze(
                    image = image,
                    lastRecognizedKanji = _uiState.value.kanji
                ) ?: return@launch

                _uiState.update { withMetadata(it.copy(kanji = newKanji)) }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { it.copy(userMessage = UiText(R.string.scan_error)) }
            }
        }.invokeOnCompletion {
            // Also runs if the scope was cancelled before the coroutine started.
            imageProxy.close()
            isProcessingFrame.set(false)
        }
    }

    fun onKanjiLongClick(kanji: Char) {
        viewModelScope.launch(analysisDispatcher) {
            _uiEvent.emit(ScanUiEvent.CopyKanji(kanji))
        }
    }

    fun googleAiSearchUrl(kanji: String): String = googleAISearch(kanji)

    fun toggleLearnedKanji(kanji: Char) {
        viewModelScope.launch(analysisDispatcher) {
            try {
                val isLearned = _uiState.value.learnedKanjis.contains(kanji)
                val isDownloadComplete = kanjiRepository.isKanjiDownloadComplete()
                _uiState.update { it.copy(isKanjiDownloadComplete = isDownloadComplete) }
                if (!isLearned && !isDownloadComplete) {
                    _uiEvent.emit(ScanUiEvent.KanjiDownloadNotCompleted)
                    return@launch
                }
                kanjiRepository.toggleLearnedKanji(kanji.toString())
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(userMessage = exception.toProgressErrorMessage()) }
            }
        }
    }

    private fun withMetadata(state: CameraScanUiState): CameraScanUiState {
        val characters = state.kanji.toSet()
        val recognized = catalog.filter { it.kanji.firstOrNull() in characters }
        return state.copy(
            recognizedJoyoKanjis = recognized.associateBy { it.kanji.first() },
            learnedKanjis = recognized.filter { it.isLearned }.mapTo(mutableSetOf()) { it.kanji.first() }
        )
    }

}
