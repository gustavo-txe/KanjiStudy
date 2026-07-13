package com.app.kanjistudy.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
interface KanjiDao {

    @Query("SELECT * FROM kanji_table")
    suspend fun getAllKanjis(): List<KanjiEntity>

    @Query("SELECT COUNT(*) FROM kanji_table")
    suspend fun countKanjis(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(kanjis: List<KanjiEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(kanjis: List<KanjiEntity>)

    @Query("UPDATE kanji_table SET isLearned = 1 WHERE kanji = :kanji")
    suspend fun markAsLearned(kanji: String): Int

    @Query("UPDATE kanji_table SET isLearned = 0 WHERE kanji = :kanji")
    suspend fun uncheckLearnedKanji(kanji: String): Int

    @Query("UPDATE kanji_table SET isLearned = CASE WHEN isLearned = 1 THEN 0 ELSE 1 END WHERE kanji = :kanji")
    suspend fun toggleLearned(kanji: String): Int

    @Query("SELECT isLearned FROM kanji_table WHERE kanji = :kanji LIMIT 1")
    suspend fun isKanjiLearned(kanji: String): Boolean

    @Query("SELECT * FROM kanji_table WHERE isLearned = 1")
    fun getLearnedKanjis(): Flow<List<KanjiEntity>>

    @Query("SELECT * FROM kanji_table WHERE kanji IN (:kanjis)")
    suspend fun getKanjisByChars(kanjis: List<String>): List<KanjiEntity>

    @Query("SELECT COUNT(*) FROM kanji_table WHERE jlpt IS NULL")
    suspend fun countKanjisMissingJlpt(): Int

    @Query("SELECT * FROM kanji_table WHERE isLearned = 1")
    suspend fun getLearnedKanjisOnce(): List<KanjiEntity>

    @Query("SELECT kanji FROM kanji_table WHERE kanji IN (:kanjis)")
    suspend fun getExistingKanjiChars(kanjis: List<String>): List<String>

    @Query("UPDATE kanji_table SET isLearned = 1 WHERE kanji IN (:kanjis)")
    suspend fun markKanjisAsLearned(kanjis: List<String>): Int

    @Transaction
    suspend fun importLearnedKanjis(kanjis: List<String>): Int {
        if (kanjis.isEmpty()) return 0
        return kanjis.chunked(SQLITE_BIND_PARAMETER_LIMIT).sumOf { chunk ->
            markKanjisAsLearned(chunk)
        }
    }

    private companion object {
        const val SQLITE_BIND_PARAMETER_LIMIT = 500
    }

}
