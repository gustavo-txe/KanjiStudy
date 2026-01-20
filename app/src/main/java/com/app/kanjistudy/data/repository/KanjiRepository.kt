package com.app.kanjistudy.data.repository

import android.content.Context
import com.app.kanjistudy.data.model.KanjiData
import com.app.kanjistudy.data.local.KanjiDao
import com.app.kanjistudy.data.remote.KanjiApiService
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class KanjiRepository @Inject constructor(private val kanjiDao: KanjiDao,
                                          private val api: KanjiApiService,
                                          @ApplicationContext private val context: Context
) {

    suspend fun getJoyoKanjis(): List<String> {
        return api.getJoyoKanjis()
    }

    suspend fun getKanjiData(kanji: String): KanjiData {

        val local = kanjiDao.getKanji(kanji)
        return if (local != null) {
            local
        }else{
            val remote = api.getReadingMeaning(kanji)
            //kanjiDao.insertKanji(remote)
            remote
        }

    }

    suspend fun getKanji(kanji : String): KanjiData { return kanjiDao.getKanji(kanji)!! }

    suspend fun getReadingMeaning(kanji: String): KanjiData { return api.getReadingMeaning(kanji) }

    suspend fun insertAllKanjis(kanjiList: List<KanjiData>) { kanjiDao.insertAll(kanjiList) }

    suspend fun getAllLocalKanjis(): List<KanjiData> { return kanjiDao.getAllKanjis() }

     fun getLearnedKanjis(): Flow<List<KanjiData>> = kanjiDao.getLearnedKanjis()

    suspend fun markAsLearned(kanji: String) { kanjiDao.markAsLearned(kanji) }

    suspend fun uncheckLearnedKanji(kanji: String) { kanjiDao.uncheckLearnedKanji(kanji) }

    suspend fun countKanjis(): Int { return kanjiDao.countKanjis() }


}