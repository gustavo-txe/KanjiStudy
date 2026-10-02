package com.app.kanjistudy.presentation.backup

import com.app.kanjistudy.core.ui.UiText

data class IOKanjiUiState(
    val isBusy: Boolean = false,
    val busyMessage: UiText? = null,
    val userMessage: UiText? = null
)
