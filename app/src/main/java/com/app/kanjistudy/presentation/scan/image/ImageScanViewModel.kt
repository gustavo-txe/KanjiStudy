package com.app.kanjistudy.presentation.scan.image

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.R
import com.app.kanjistudy.core.ui.UiText
import com.app.kanjistudy.core.ui.toProgressErrorMessage
import com.app.kanjistudy.data.image.GalleryImageLoader
import com.app.kanjistudy.data.ocr.ImageKanjiScanner
import com.app.kanjistudy.data.repository.KanjiRepository
import com.app.kanjistudy.di.DefaultDispatcher
import com.app.kanjistudy.domain.model.Kanji
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
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
class ImageScanViewModel @Inject constructor(
    private val imageLoader: GalleryImageLoader,
    private val scanner: ImageKanjiScanner,
    private val kanjiRepository: KanjiRepository,
    @DefaultDispatcher dispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageScanUiState())
    val uiState: StateFlow<ImageScanUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<ImageKanjiScanUiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()
    @OptIn(ExperimentalCoroutinesApi::class)
    private val scanDispatcher = dispatcher.limitedParallelism(1)

    private var scanJob: Job? = null
    private val scanVersion = AtomicLong(0L)

    private var catalog: List<Kanji> = emptyList()

    init {
        viewModelScope.launch(scanDispatcher) {
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

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun onScanImageClick() {
        if (_uiState.value.isSelectingImage || _uiState.value.isLoading) return

        _uiState.update { it.copy(isSelectingImage = true) }
        viewModelScope.launch {
            _uiEvent.emit(ImageKanjiScanUiEvent.LaunchDocumentScanner)
        }
    }

    fun onImagePickerFinished() {
        _uiState.update { it.copy(isSelectingImage = false) }
    }

    fun onImageSelected(uri: Uri) {
        _uiState.update {
            it.copy(
                selectedImageUri = uri,
                isSelectingImage = false,
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
            try {
                val isLearned = _uiState.value.learnedKanjis.contains(kanji)
                val isDownloadComplete = kanjiRepository.isKanjiDownloadComplete()
                _uiState.update { it.copy(isKanjiDownloadComplete = isDownloadComplete) }
                if (!isLearned && !isDownloadComplete) {
                    _uiEvent.emit(ImageKanjiScanUiEvent.KanjiDownloadNotCompleted)
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

    private fun scanImage(imageUri: Uri) {
        val version = scanVersion.incrementAndGet()
        scanJob?.cancel()
        scanJob = viewModelScope.launch(scanDispatcher) {
            _uiState.update {
                it.copy(
                    isSelectingImage = false,
                    isLoading = true,
                    message = null,
                    recognizedKanji = "",
                    recognizedJoyoKanjis = emptyMap(),
                    learnedKanjis = emptySet(),
                )
            }

            try {
                val inputImage = imageLoader.load(imageUri)
                val kanji = scanner(inputImage)
                if (version != scanVersion.get()) return@launch

                _uiState.update {
                    withMetadata(
                        it.copy(
                            isLoading = false,
                            recognizedKanji = kanji,
                            message = if (kanji.isBlank()) {
                                UiText(R.string.scan_empty)
                            } else {
                                UiText(R.string.scan_success)
                            },
                        )
                    )
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                if (version != scanVersion.get()) return@launch

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        message = UiText(R.string.scan_error),
                    )
                }
            }
        }
    }

    private fun withMetadata(state: ImageScanUiState): ImageScanUiState {
        val characters = state.recognizedKanji.toSet()
        val recognized = catalog.filter { it.kanji.firstOrNull() in characters }
        return state.copy(
            recognizedJoyoKanjis = recognized.associateBy { it.kanji.first() },
            learnedKanjis = recognized.filter { it.isLearned }.mapTo(mutableSetOf()) { it.kanji.first() }
        )
    }

}

sealed interface ImageKanjiScanUiEvent {
    data object LaunchDocumentScanner : ImageKanjiScanUiEvent
    data object KanjiDownloadNotCompleted : ImageKanjiScanUiEvent
}
