package com.app.kanjistudy.learned.io

data class IOKanjiUiState(
    val isBusy: Boolean = false,
    val busyMessage: String? = null,
    val userMessage: String? = null
)