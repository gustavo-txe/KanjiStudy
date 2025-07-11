package com.app.kanjistudy.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.app.kanjistudy.KanjiData

@Database(entities = [KanjiData::class], version = 1, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun kanjiDao(): KanjiDao

}