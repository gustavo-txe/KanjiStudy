package com.app.kanjistudy.presentation.home

import com.app.kanjistudy.core.ui.UiText

data class KanjiUiState(
    val userMessage: UiText? = null,
    val isLoading: Boolean = true,
    val isCatalogReady: Boolean = false,
    val loadingProgress: Float = 0f,
    val joyoKanjis: List<String> = emptyList(),
    val filteredKanjis: List<String> = emptyList(),
    val kunReadings: Map<String, List<String>> = emptyMap(),
    val onReadings: Map<String, List<String>> = emptyMap(),
    val meanings: Map<String, List<String>> = emptyMap(),
    val jlptLevels: Map<String, Int?> = emptyMap(),
    val queryInput: String = "",
    val submittedQuery: String = "",
    val selectedJlptLevel: Int? = null,
    val error: UiText? = null,

)
