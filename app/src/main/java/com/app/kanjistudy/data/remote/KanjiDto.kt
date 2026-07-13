package com.app.kanjistudy.data.remote

import com.google.gson.annotations.SerializedName

data class KanjiDto(
    val kanji: String,
    val jlpt: Int? = null,
    @SerializedName("kun_readings")
    val kunReadings: List<String>,
    @SerializedName("on_readings")
    val onReadings: List<String>,
    val meanings: List<String>
)
