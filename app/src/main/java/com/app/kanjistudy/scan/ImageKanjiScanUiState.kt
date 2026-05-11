package com.app.kanjistudy.scan

import android.net.Uri

data class ImageKanjiScanUiState(
    val selectedImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val recognizedKanji: String = "",
    val message: String? = null,
)