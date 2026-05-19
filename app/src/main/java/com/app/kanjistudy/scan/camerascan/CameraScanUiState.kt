package com.app.kanjistudy.scan.camerascan

import com.app.kanjistudy.data.model.KanjiData

data class CameraScanUiState (
    val isPaused: Boolean = false,
    val kanji: String = "",
    val recognizedJoyoKanjis: Map<Char, KanjiData> = emptyMap(),
    val learnedKanjis: Set<Char> = emptySet()
)