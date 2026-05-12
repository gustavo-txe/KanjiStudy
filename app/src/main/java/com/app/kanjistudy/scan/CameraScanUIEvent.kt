package com.app.kanjistudy.scan

sealed class ScanUiEvent {
    data class ShowGoogleDialog(val kanji: Char) : ScanUiEvent()
    data class CopyKanji(val kanji: Char) : ScanUiEvent()
}