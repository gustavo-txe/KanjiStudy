package com.app.kanjistudy.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.google.gson.annotations.SerializedName

@Entity(tableName = "kanji_table")
data class KanjiData(
    @PrimaryKey
    val kanji: String,
    @SerializedName("kun_readings") val kunReadings: List<String>,
    @SerializedName("on_readings") val onReadings: List<String>,
    val meanings: List<String>,
    val isLearned: Boolean = false
)