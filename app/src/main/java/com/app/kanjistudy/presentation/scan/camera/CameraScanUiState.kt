package com.app.kanjistudy.presentation.scan.camera

import com.app.kanjistudy.core.ui.UiText
import com.app.kanjistudy.domain.model.Kanji

data class CameraScanUiState (
    val userMessage: UiText? = null,
    val isPaused: Boolean = false,
    val kanji: String = "",
    val recognizedJoyoKanjis: Map<Char, Kanji> = emptyMap(),
    val learnedKanjis: Set<Char> = emptySet(),
    val isKanjiDownloadComplete: Boolean = false
)
