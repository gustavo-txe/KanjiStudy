package com.app.kanjistudy.home.kanjis

sealed class KanjiUiEvent {
    data class ShowDialog(val dialog: KanjiDialog) : KanjiUiEvent()
}