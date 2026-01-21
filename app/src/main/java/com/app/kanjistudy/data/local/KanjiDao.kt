package com.app.kanjistudy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.kanjistudy.data.model.KanjiData
import kotlinx.coroutines.flow.Flow

@Dao
interface KanjiDao {

    @Query("SELECT * FROM kanji_table")
    suspend fun getAllKanjis(): List<KanjiData>

    @Query("SELECT COUNT(*) FROM kanji_table")
    suspend fun countKanjis(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(kanjis: List<KanjiData>)

    @Query("UPDATE kanji_table SET isLearned = 1 WHERE kanji = :kanji")
    suspend fun markAsLearned(kanji: String) : Int

    @Query("UPDATE kanji_table SET isLearned = 0 WHERE kanji = :kanji")
    suspend fun uncheckLearnedKanji(kanji: String) : Int

    @Query("SELECT * FROM kanji_table WHERE isLearned = 1")
     fun getLearnedKanjis(): Flow<List<KanjiData>>

}