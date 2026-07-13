package com.app.kanjistudy.data.mapper

import com.app.kanjistudy.data.local.KanjiEntity
import com.app.kanjistudy.data.remote.KanjiDto
import com.app.kanjistudy.domain.model.Kanji

fun KanjiEntity.toDomain(): Kanji = Kanji(
    kanji = kanji,
    jlpt = jlpt,
    kunReadings = kunReadings,
    onReadings = onReadings,
    meanings = meanings,
    isLearned = isLearned
)

fun KanjiDto.toEntity(isLearned: Boolean = false): KanjiEntity = KanjiEntity(
    kanji = kanji,
    jlpt = jlpt,
    kunReadings = kunReadings,
    onReadings = onReadings,
    meanings = meanings,
    isLearned = isLearned
)
