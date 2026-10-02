package com.app.kanjistudy.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "kanji_table")
data class KanjiEntity(
    @PrimaryKey
    val kanji: String,
    val jlpt: Int? = null,
    val kunReadings: List<String>,
    val onReadings: List<String>,
    val meanings: List<String>,
    val isLearned: Boolean = false
)
