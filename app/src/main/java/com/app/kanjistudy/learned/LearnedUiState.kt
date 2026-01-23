package com.app.kanjistudy.learned

import com.app.kanjistudy.data.model.KanjiData

data class LearnedUiState(
    val kanjis: List<KanjiData> = emptyList(),
    val filteredKanjis: List<KanjiData> = emptyList(),
    val query: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)