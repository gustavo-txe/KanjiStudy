package com.app.kanjistudy.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [KanjiEntity::class, ProgressState::class], version = 3, exportSchema = false)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun kanjiDao(): KanjiDao

    companion object {

        const val DATABASE_VERSION = 3

        val MIGRATION_2_3: Migration = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS progress_state (id INTEGER NOT NULL PRIMARY KEY)")
                db.execSQL("INSERT INTO progress_state (id) SELECT 0 WHERE (SELECT COUNT(*) FROM kanji_table) >= 2136 OR EXISTS (SELECT 1 FROM kanji_table WHERE isLearned = 1)")
            }
        }

        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE kanji_table ADD COLUMN jlpt INTEGER")
            }
        }
    }
}
