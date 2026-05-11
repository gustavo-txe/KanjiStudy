package com.app.kanjistudy.scan

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.scan.usecase.ScanKanjiFromGalleryUseCase
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ImageKanjiScanViewModel @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val scanKanjiFromGalleryUseCase: ScanKanjiFromGalleryUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ImageKanjiScanUiState())
    val uiState: StateFlow<ImageKanjiScanUiState> = _uiState.asStateFlow()

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
        _uiState.value = ImageKanjiScanUiState()
    }

    private fun scanImage() {
        val imageUri = _uiState.value.selectedImageUri ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, message = null, recognizedKanji = "") }
            try {
                val inputImage = InputImage.fromFilePath(applicationContext, imageUri)
                val kanji = scanKanjiFromGalleryUseCase(inputImage)

                _uiState.update {
                    it.copy(
                        isLoading = false,
                        recognizedKanji = kanji,
                        message = if (kanji.isBlank()) "No kanji was identified." else "Identified kanji:",
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isLoading = false, message = "Could not process this image. Please try another one.")
                }
            }
        }
    }
}