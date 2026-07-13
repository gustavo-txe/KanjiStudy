package com.app.kanjistudy.scan.camerascan

import com.app.kanjistudy.domain.model.Kanji

data class CameraScanUiState (
    val isPaused: Boolean = false,
    val kanji: String = "",
    val recognizedJoyoKanjis: Map<Char, Kanji> = emptyMap(),
    val learnedKanjis: Set<Char> = emptySet(),
    val isKanjiDownloadComplete: Boolean = false
)