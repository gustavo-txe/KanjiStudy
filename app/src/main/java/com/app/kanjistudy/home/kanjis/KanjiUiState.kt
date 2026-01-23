package com.app.kanjistudy.home.kanjis

data class KanjiUiState(
    val isLoading: Boolean = true,
    val loadingProgress: Float = 0f,
    val joyoKanjis: List<String> = emptyList(),
    val filteredKanjis: List<String> = emptyList(),
    val kunReadings: Map<String, List<String>> = emptyMap(),
    val onReadings: Map<String, List<String>> = emptyMap(),
    val meanings: Map<String, List<String>> = emptyMap(),
    var query: String = "",
    val error: String? = null,

)