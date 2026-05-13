package com.app.kanjistudy.scan

sealed class ScanUiEvent {
    data class CopyKanji(val kanji: Char) : ScanUiEvent()
}