package com.app.kanjistudy.scan.camerascan

sealed class ScanUiEvent {
    data class CopyKanji(val kanji: Char) : ScanUiEvent()
    data object KanjiDownloadNotCompleted : ScanUiEvent()
}