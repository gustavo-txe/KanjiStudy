package com.app.kanjistudy

import android.util.Log
import com.app.kanjistudy.room.KanjiDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import javax.inject.Inject

class KanjiRepository @Inject constructor(private val kanjiDao: KanjiDao,
                                          private val api: KanjiApiService) {

    suspend fun getJoyoKanjis(): List<String> {
        return api.getJoyoKanjis()
    }

    suspend fun getJoyoKunReading(kanji: String): List<String> {
        return api.getReadingMeaning(kanji).kunReadings
    }

    suspend fun getJoyoOnReading(kanji: String): List<String> {
        return api.getReadingMeaning(kanji).onReadings
    }

    suspend fun getJoyoKanjiMeanings(kanji: String): List<String> {
        return api.getReadingMeaning(kanji).meanings
    }



}