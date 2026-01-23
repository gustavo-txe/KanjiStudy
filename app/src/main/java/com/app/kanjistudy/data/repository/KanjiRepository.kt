package com.app.kanjistudy.data.repository

import com.app.kanjistudy.data.model.KanjiData
import com.app.kanjistudy.data.local.KanjiDao
import com.app.kanjistudy.data.remote.KanjiApiService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import javax.inject.Inject

class KanjiRepository @Inject constructor(
    private val kanjiDao: KanjiDao,
    private val api: KanjiApiService,
) {
    suspend fun ensureAllKanjisLoaded(
        onProgress: (Float) -> Unit
    ): List<KanjiData> = withContext(Dispatchers.IO) {

        val count = kanjiDao.countKanjis()
        if (count >= 2140) {
            return@withContext kanjiDao.getAllKanjis()
        }

        val joyoKanjis = api.getJoyoKanjis()
        val total = joyoKanjis.size
        var processed = 0

        joyoKanjis.chunked(40).forEach { chunk ->
            val kanjiData = coroutineScope {
                chunk.map { kanji ->
                    async {
                        runCatching { api.getReadingMeaning(kanji) }.getOrNull()
                    }
                }.awaitAll().filterNotNull()
            }

            kanjiDao.insertAll(kanjiData)

            processed += chunk.size
            onProgress(processed.toFloat() / total)
        }

        kanjiDao.getAllKanjis()
    }

    suspend fun toggleLearnedKanji(kanji: String) = withContext(Dispatchers.IO) {
        val isLearned = kanjiDao.isKanjiLearned(kanji)
        if (isLearned) kanjiDao.uncheckLearnedKanji(kanji)
        else kanjiDao.markAsLearned(kanji)
    }

    fun getLearnedKanjis(): Flow<List<KanjiData>> = kanjiDao.getLearnedKanjis()

}