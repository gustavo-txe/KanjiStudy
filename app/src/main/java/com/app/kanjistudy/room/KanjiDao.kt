package com.app.kanjistudy.room

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.app.kanjistudy.KanjiData
import kotlinx.coroutines.flow.Flow

@Dao
interface KanjiDao {

    @Query("SELECT * FROM kanji_table")
    fun getAllKanjis(): Flow<List<KanjiData>>

    @Query("SELECT * FROM kanji_table WHERE kanji = :kanji")
    fun getKanji(kanji: String): KanjiData

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertAll(kanjiData: List<KanjiData>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertKanji(kanji: KanjiData)

    @Query("SELECT * FROM kanji_table")
    fun getAllKanjisFlow(): Flow<List<KanjiData>>

}