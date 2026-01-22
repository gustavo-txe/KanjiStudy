package com.app.kanjistudy.viewmodel

import android.annotation.SuppressLint
import androidx.camera.core.ImageProxy
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.ScanKanjiUseCase
import com.app.kanjistudy.ScanUiEvent
import com.app.kanjistudy.ScanUiState
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
class ScanViewModel @Inject constructor(
    private val scanKanjiUseCase: ScanKanjiUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ScanUiState())
    val uiState: StateFlow<ScanUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ScanUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    fun togglePause() {
        _uiState.update { it.copy(isPaused = !it.isPaused) }
    }

    @SuppressLint("UnsafeOptInUsageError")
    fun onFrame(imageProxy: ImageProxy) {
        if (_uiState.value.isPaused) return
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
                    isPaused = _uiState.value.isPaused,
                    lastRecognizedKanji = _uiState.value.kanji
                ) ?: return@launch

                _uiState.update {
                    it.copy(kanji = newKanji)
                }
            } catch (_: Exception) {
            } finally {
                imageProxy.close()
            }
        }
    }


    fun onKanjiClick(kanji: Char) {
        viewModelScope.launch {
            _uiEvent.emit(ScanUiEvent.ShowGoogleDialog(kanji))
        }
    }

    fun onKanjiLongClick(kanji: Char) {
        viewModelScope.launch {
            _uiEvent.emit(ScanUiEvent.CopyKanji(kanji))
        }
    }

}
