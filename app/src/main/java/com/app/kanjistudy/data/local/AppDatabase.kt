package com.app.kanjistudy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.app.kanjistudy.data.model.KanjiData

@Database(entities = [KanjiData::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun kanjiDao(): KanjiDao

    companion object {

        const val DATABASE_VERSION = 2

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE kanji_table ADD COLUMN jlpt INTEGER")
            }
        }
    }
}