package com.app.kanjistudy.presentation.scan.camera

sealed class ScanUiEvent {
    data class CopyKanji(val kanji: Char) : ScanUiEvent()
    data object KanjiDownloadNotCompleted : ScanUiEvent()
}
