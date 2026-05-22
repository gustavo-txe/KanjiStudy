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
import kotlinx.coroutines.delay
import javax.inject.Inject

class KanjiRepository @Inject constructor(
    private val kanjiDao: KanjiDao,
    private val api: KanjiApiService,
) {

    private companion object {
        const val MIN_KANJI_COUNT = 2136
        const val CHUNK_SIZE = 40
        const val MAX_RETRIES = 3
        const val RETRY_DELAY_MS = 400L
    }

    suspend fun ensureAllKanjisLoaded(
        onProgress: (Float) -> Unit
    ): List<KanjiData> = withContext(Dispatchers.IO) {

        val count = kanjiDao.countKanjis()
        if (count >= MIN_KANJI_COUNT) {
            return@withContext kanjiDao.getAllKanjis()
        }

        val joyoKanjis = api.getJoyoKanjis()
        val total = joyoKanjis.size
        var processed = 0

        joyoKanjis.chunked(CHUNK_SIZE).forEach { chunk ->
            val kanjiData = coroutineScope {
                chunk.map { kanji ->
                    async {
                        fetchReadingMeaningWithRetry(kanji)
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

    suspend fun getKanjisByChars(kanjis: Set<Char>): List<KanjiData> = withContext(Dispatchers.IO) {
        if (kanjis.isEmpty()) return@withContext emptyList()
        kanjiDao.getKanjisByChars(kanjis.map { it.toString() })
    }

    suspend fun isKanjiDownloadComplete(): Boolean = withContext(Dispatchers.IO) {
        kanjiDao.countKanjis() >= MIN_KANJI_COUNT
    }

    private suspend fun fetchReadingMeaningWithRetry(kanji: String): KanjiData? {
        repeat(MAX_RETRIES) { attempt ->
            runCatching {
                return api.getReadingMeaning(kanji)
            }

            if (attempt < MAX_RETRIES - 1) {
                delay(RETRY_DELAY_MS * (attempt + 1))
            }
        }

        return null
    }

}