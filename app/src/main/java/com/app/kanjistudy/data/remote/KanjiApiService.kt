package com.app.kanjistudy.data.remote

import com.app.kanjistudy.data.model.KanjiData
import retrofit2.http.GET
import retrofit2.http.Path

interface KanjiApiService {
    @GET("kanji/joyo")
    suspend fun getJoyoKanjis(): List<String>

    @GET("kanji/{kanji}")
    suspend fun getReadingMeaning(@Path("kanji") kanji: String): KanjiData

}