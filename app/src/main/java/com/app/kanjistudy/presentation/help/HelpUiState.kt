package com.app.kanjistudy.presentation.help

data class HelpFeature(
    @androidx.annotation.StringRes val title: Int,
    @androidx.annotation.StringRes val description: Int,
    @androidx.annotation.StringRes val howToUse: Int
)

data class HelpUiState(
    val features: List<HelpFeature> = emptyList()
)
