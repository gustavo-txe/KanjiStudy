package com.app.kanjistudy.scan.imagescan

import android.net.Uri
import com.app.kanjistudy.domain.model.Kanji

data class ImageScanUiState(
    val selectedImageUri: Uri? = null,
    val isLoading: Boolean = false,
    val recognizedKanji: String = "",
    val message: String? = null,
    val recognizedJoyoKanjis: Map<Char, Kanji> = emptyMap(),
    val learnedKanjis: Set<Char> = emptySet(),
    val isKanjiDownloadComplete: Boolean = false,
)