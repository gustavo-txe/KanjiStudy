package com.app.kanjistudy.scan

import android.net.Uri
import com.app.kanjistudy.data.model.KanjiData

data class ImageScanUiState(
    val selectedImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val recognizedKanji: String = "",
    val message: String? = null,
    val recognizedJoyoKanjis: Map<Char, KanjiData> = emptyMap(),
    val learnedKanjis: Set<Char> = emptySet(),
)