package com.app.kanjistudy.scan

import android.annotation.SuppressLint
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.data.repository.KanjiRepository
import com.app.kanjistudy.scan.usecase.ScanKanjiUseCase
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
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

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun onFrame(imageProxy: ImageProxy) {
        if (_uiState.value.isPaused) {
            imageProxy.close()
            return
        }
        val mediaImage = imageProxy.image ?: run {
            imageProxy.close()
            return
        }

        val image = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )

        viewModelScope.launch {
            try {
                val newKanji = scanKanjiUseCase.analyze(
                    image = image,
                    lastRecognizedKanji = _uiState.value.kanji
                ) ?: return@launch

                _uiState.update {
                    it.copy(kanji = newKanji)
                }
                refreshRecognizedMetadata(newKanji)
            } catch (_: Exception) {
            } finally {
                imageProxy.close()
            }
        }
    }


    fun onKanjiLongClick(kanji: Char) {
        viewModelScope.launch {
            _uiEvent.emit(ScanUiEvent.CopyKanji(kanji))
        }
    }

    private suspend fun refreshRecognizedMetadata(recognizedText: String) {
        val uniqueKanjis = recognizedText.toSet()
        val joyoKanjis = kanjiRepository.getKanjisByChars(uniqueKanjis)
        val joyoMap = joyoKanjis.associateBy { it.kanji.first() }
        val learned = joyoKanjis.filter { it.isLearned }.mapTo(mutableSetOf()) { it.kanji.first() }

        _uiState.update {
            it.copy(
                recognizedJoyoKanjis = joyoMap,
                learnedKanjis = learned
            )
        }
    }

}