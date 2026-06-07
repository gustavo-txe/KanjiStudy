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
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicLong
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

    @OptIn(ExperimentalCoroutinesApi::class)
    private val scanDispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(1)

    private var scanJob: Job? = null
    private val scanVersion = AtomicLong(0L)

    init {
        viewModelScope.launch(scanDispatcher) {
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
                recognizedJoyoKanjis = emptyMap(),
                learnedKanjis = emptySet(),
            )
        }
        scanImage(uri)
    }

    fun retryScan() {
        scanImage(_uiState.value.selectedImageUri ?: return)
    }

    fun removeImage() {
        scanVersion.incrementAndGet()
        scanJob?.cancel()
        _uiState.update {
            ImageScanUiState(isKanjiDownloadComplete = it.isKanjiDownloadComplete)
        }
    }

    fun toggleLearnedKanji(kanji: Char) {
        viewModelScope.launch(scanDispatcher) {
            val isLearned = _uiState.value.learnedKanjis.contains(kanji)
            val isDownloadComplete = kanjiRepository.isKanjiDownloadComplete()
            _uiState.update { it.copy(isKanjiDownloadComplete = isDownloadComplete) }

            if (!isLearned && !isDownloadComplete) {
                _uiEvent.emit(ImageKanjiScanUiEvent.KanjiDownloadNotCompleted)
                return@launch
            }

            kanjiRepository.toggleLearnedKanji(kanji.toString())
            refreshRecognizedMetadata(_uiState.value.recognizedKanji)
        }
    }

    private fun scanImage(imageUri: Uri) {
        val version = scanVersion.incrementAndGet()
        scanJob?.cancel()
        scanJob = viewModelScope.launch(scanDispatcher) {
            _uiState.update {
                it.copy(
                    isLoading = true,
                    message = null,
                    recognizedKanji = "",
                    recognizedJoyoKanjis = emptyMap(),
                    learnedKanjis = emptySet(),
                )
            }

            runCatching {
                val inputImage = withContext(Dispatchers.IO) {
                    InputImage.fromFilePath(applicationContext, imageUri)
                }
                imageScanFromGallery(inputImage)
            }.onSuccess { kanji ->
                if (version != scanVersion.get()) return@onSuccess

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recognizedKanji = kanji,
                        message = if (kanji.isBlank()) {
                            "No kanji were identified."
                        } else {
                            "Identified kanji"
                        },
                    )
                }
                refreshRecognizedMetadata(kanji)
            }.onFailure { error ->
                if (error is CancellationException) throw error
                if (version != scanVersion.get()) return@onFailure

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = "Could not process this image. Please try another one.",
                    )
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
                learnedKanjis = learned,
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
