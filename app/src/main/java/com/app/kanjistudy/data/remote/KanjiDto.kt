package com.app.kanjistudy.data.remote

import com.google.gson.annotations.SerializedName

data class KanjiDto(
    @SerializedName("kanji")
    val kanji: String,
    @SerializedName("jlpt")
    val jlpt: Int? = null,
    @SerializedName("kun_readings")
    val kunReadings: List<String>,
    @SerializedName("on_readings")
    val onReadings: List<String>,
    @SerializedName("meanings")
    val meanings: List<String>
)
