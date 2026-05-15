package com.app.kanjistudy.help

data class HelpFeature(
    val title: String,
    val description: String,
    val howToUse: String
)

data class HelpUiState(
    val features: List<HelpFeature> = emptyList()
)