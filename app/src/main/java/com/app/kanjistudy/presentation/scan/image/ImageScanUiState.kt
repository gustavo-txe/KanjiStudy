package com.app.kanjistudy.presentation.scan.image

import android.net.Uri
import com.app.kanjistudy.core.ui.UiText
import com.app.kanjistudy.domain.model.Kanji

data class ImageScanUiState(
    val userMessage: UiText? = null,
    val selectedImageUri: Uri? = null,
    val isSelectingImage: Boolean = false,
    val isLoading: Boolean = false,
    val recognizedKanji: String = "",
    val message: UiText? = null,
    val recognizedJoyoKanjis: Map<Char, Kanji> = emptyMap(),
    val learnedKanjis: Set<Char> = emptySet(),
    val isKanjiDownloadComplete: Boolean = false,
)
