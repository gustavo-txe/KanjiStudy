package com.app.kanjistudy.domain.model

data class Kanji(
    val kanji: String,
    val jlpt: Int? = null,
    val kunReadings: List<String>,
    val onReadings: List<String>,
    val meanings: List<String>,
    val isLearned: Boolean = false
)
