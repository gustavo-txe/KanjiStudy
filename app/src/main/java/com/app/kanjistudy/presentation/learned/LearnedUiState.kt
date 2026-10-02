package com.app.kanjistudy.presentation.learned

import com.app.kanjistudy.core.ui.UiText
import com.app.kanjistudy.domain.model.Kanji

data class LearnedUiState(
    val userMessage: UiText? = null,
    val kanjis: List<Kanji> = emptyList(),
    val filteredKanjis: List<Kanji> = emptyList(),
    val query: String = "",
    val selectedJlptLevel: Int? = null,
    val isLoading: Boolean = false,
    val error: UiText? = null
)
