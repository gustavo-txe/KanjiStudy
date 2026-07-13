package com.app.kanjistudy.learned

import com.app.kanjistudy.domain.model.Kanji

data class LearnedUiState(
    val kanjis: List<Kanji> = emptyList(),
    val filteredKanjis: List<Kanji> = emptyList(),
    val query: String = "",
    val selectedJlptLevel: Int? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)