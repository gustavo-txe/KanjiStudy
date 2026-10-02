package com.app.kanjistudy.testing

import com.app.kanjistudy.data.local.KanjiEntity
import com.app.kanjistudy.domain.model.Kanji

fun kanji(char: String = "日", level: Int? = 5, learned: Boolean = false) = Kanji(
    kanji = char,
    jlpt = level,
    kunReadings = listOf("ひ"),
    onReadings = listOf("ニチ"),
    meanings = listOf("sun", "day"),
    isLearned = learned,
)

fun entity(char: String = "日", learned: Boolean = false) = KanjiEntity(
    kanji = char,
    jlpt = 5,
    kunReadings = listOf("ひ"),
    onReadings = listOf("ニチ"),
    meanings = listOf("sun", "day"),
    isLearned = learned,
)
