package com.app.kanjistudy.scan.camerascan

import android.annotation.SuppressLint
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.data.repository.KanjiRepository
import com.app.kanjistudy.scan.usecase.ScanKanjiUseCase
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject

@HiltViewModel
class CameraScanViewModel @Inject constructor(
    private val scanKanjiUseCase: ScanKanjiUseCase,
    private val kanjiRepository: KanjiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CameraScanUiState())
    val uiState: StateFlow<CameraScanUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ScanUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    private val isProcessingFrame = AtomicBoolean(false)

    private val lastFrameAcceptedAtMs = AtomicLong(0L)

    @OptIn(ExperimentalCoroutinesApi::class)
    private val analysisDispatcher = Dispatchers.Default.limitedParallelism(1)

    private companion object {
        const val FRAME_ANALYSIS_INTERVAL_MS = 350L
    }

    init {
        viewModelScope.launch(analysisDispatcher) {
            refreshDownloadStatus()
        }
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
        val now = System.currentTimeMillis()
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

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        viewModelScope.launch(analysisDispatcher) {
            try {
                val newKanji = scanKanjiUseCase.analyze(
                    image = image,
                    lastRecognizedKanji = _uiState.value.kanji
                ) ?: return@launch

                withContext(Dispatchers.Main.immediate) {
                    _uiState.update { it.copy(kanji = newKanji) }
                }
                refreshRecognizedMetadata(newKanji)
            } catch (_: Exception) {
            } finally {
                isProcessingFrame.set(false)
                imageProxy.close()
            }
        }
    }


    fun onKanjiLongClick(kanji: Char) {
        viewModelScope.launch(analysisDispatcher) {
            _uiEvent.emit(ScanUiEvent.CopyKanji(kanji))
        }
    }

    fun toggleLearnedKanji(kanji: Char) {
        viewModelScope.launch(analysisDispatcher) {
            val isLearned = _uiState.value.learnedKanjis.contains(kanji)
            val isDownloadComplete = kanjiRepository.isKanjiDownloadComplete()
            _uiState.update { it.copy(isKanjiDownloadComplete = isDownloadComplete) }
            if (!isLearned && !isDownloadComplete) {
                _uiEvent.emit(ScanUiEvent.KanjiDownloadNotCompleted)
                return@launch
            }
            kanjiRepository.toggleLearnedKanji(kanji.toString())
            refreshRecognizedMetadata(_uiState.value.kanji)
        }
    }


    private suspend fun refreshRecognizedMetadata(recognizedText: String) {
        val uniqueKanjis = recognizedText.toSet()
        val joyoKanjis = kanjiRepository.getKanjisByChars(uniqueKanjis)
        val joyoMap = joyoKanjis.associateBy { it.kanji.first() }
        val learned = joyoKanjis.filter { it.isLearned }.mapTo(mutableSetOf()) { it.kanji.first() }

        withContext(Dispatchers.Main.immediate) {
            _uiState.update {
                it.copy(
                    recognizedJoyoKanjis = joyoMap,
                    learnedKanjis = learned
                )
            }
        }
    }

    suspend fun refreshDownloadStatus() {
        val isComplete = kanjiRepository.isKanjiDownloadComplete()
        _uiState.update { it.copy(isKanjiDownloadComplete = isComplete) }

    }

}