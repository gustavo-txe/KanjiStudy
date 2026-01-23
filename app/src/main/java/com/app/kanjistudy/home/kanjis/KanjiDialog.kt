package com.app.kanjistudy.home.kanjis

sealed class KanjiDialog {
    data class Google(val kanji: String) : KanjiDialog()
    data class ToggleLearned(val kanji: String,
                             val isLearned: Boolean
    ) : KanjiDialog()
}