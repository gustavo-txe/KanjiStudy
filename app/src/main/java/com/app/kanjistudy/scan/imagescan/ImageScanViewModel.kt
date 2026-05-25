package com.app.kanjistudy.scan.imagescan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.data.repository.KanjiRepository
import com.app.kanjistudy.scan.usecase.ImageScanFromGallery
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageScanViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val imageScanFromGallery: ImageScanFromGallery,
    private val kanjiRepository: KanjiRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageScanUiState())
    val uiState: StateFlow<ImageScanUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ImageKanjiScanUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()

    init {
        viewModelScope.launch {
            refreshDownloadStatus()
        }
    }

    fun onScanImageClick() {
        viewModelScope.launch {
            _uiEvent.emit(ImageKanjiScanUiEvent.LaunchGalleryPicker)
        }
    }

    fun onImageSelected(uri: Uri) {
        _uiState.update {
            it.copy(
                selectedImageUri = uri,
                message = null,
                recognizedKanji = "",
            )
        }
        scanImage()
    }

    fun retryScan() {
        scanImage()
    }

    fun removeImage() {
        _uiState.value = ImageScanUiState()
    }

    fun toggleLearnedKanji(kanji: Char) {
        viewModelScope.launch {
            val isLearned = _uiState.value.learnedKanjis.contains(kanji)
            val isDownloadComplete = kanjiRepository.isKanjiDownloadComplete()
            _uiState.update { it.copy(isKanjiDownloadComplete = isDownloadComplete) }

            if (!isLearned && !isDownloadComplete) {                _uiEvent.emit(ImageKanjiScanUiEvent.KanjiDownloadNotCompleted)
                return@launch
            }
            kanjiRepository.toggleLearnedKanji(kanji.toString())
            refreshRecognizedMetadata(_uiState.value.recognizedKanji)
        }
    }

    private fun scanImage() {
        val imageUri = _uiState.value.selectedImageUri ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null, recognizedKanji = "") }
            try {
                val inputImage = InputImage.fromFilePath(applicationContext, imageUri)
                val kanji = imageScanFromGallery(inputImage)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recognizedKanji = kanji,
                        message = if (kanji.isBlank()) "No kanji was identified." else "Identified kanji:",
                    )
                }
                refreshRecognizedMetadata(kanji)
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, message = "Could not process this image. Please try another one.")
                }
            }
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
    private suspend fun refreshDownloadStatus() {
        val isComplete = kanjiRepository.isKanjiDownloadComplete()
        _uiState.update { it.copy(isKanjiDownloadComplete = isComplete) }
    }
}

sealed interface ImageKanjiScanUiEvent {
    data object LaunchGalleryPicker : ImageKanjiScanUiEvent
    data object KanjiDownloadNotCompleted : ImageKanjiScanUiEvent
}