package com.app.kanjistudy.presentation.home

sealed class KanjiUiEvent {
    data class ShowDialog(val dialog: KanjiDialog) : KanjiUiEvent()
}
