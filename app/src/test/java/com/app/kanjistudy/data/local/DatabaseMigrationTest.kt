package com.app.kanjistudy.data.local

import android.app.Application
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import com.app.kanjistudy.testing.entity
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class DatabaseMigrationTest {
    @Test fun `version two upgrade preserves learned progress metadata and persists after reopening`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        for (count in listOf(40, 2136)) {
            val name = "migration-v2-learned-$count.db"
            context.deleteDatabase(name)
            context.openOrCreateDatabase(name, 0, null).use { legacy ->
                legacy.execSQL("CREATE TABLE kanji_table (kanji TEXT NOT NULL PRIMARY KEY, jlpt INTEGER, kunReadings TEXT NOT NULL, onReadings TEXT NOT NULL, meanings TEXT NOT NULL, isLearned INTEGER NOT NULL)")
                legacy.beginTransaction()
                try {
                    repeat(count) { index ->
                        legacy.execSQL(
                            "INSERT INTO kanji_table VALUES (?, 5, '[\"ひ\"]', '[\"ニチ\"]', '[\"sun\",\"day\"]', ?)",
                            arrayOf((0x4E00 + index).toChar().toString(), if (index < 2) 1 else 0)
                        )
                    }
                    legacy.setTransactionSuccessful()
                } finally {
                    legacy.endTransaction()
                }
                legacy.version = 2
            }
            val expected = listOf(entity("一", true), entity("丁", true))
            val upgraded = Room.databaseBuilder(context, AppDatabase::class.java, name)
                .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).build()
            try {
                assertEquals(count, upgraded.kanjiDao().countKanjis())
                assertEquals(expected, upgraded.kanjiDao().getLearnedKanjisOnce())
                assertTrue(upgraded.kanjiDao().isProgressInitialized())
            } finally {
                upgraded.close()
            }
            val reopened = Room.databaseBuilder(context, AppDatabase::class.java, name).build()
            try {
                assertEquals(count, reopened.kanjiDao().countKanjis())
                assertEquals(expected, reopened.kanjiDao().getLearnedKanjisOnce())
                assertTrue(reopened.kanjiDao().isProgressInitialized())
            } finally {
                reopened.close()
                context.deleteDatabase(name)
            }
        }
    }

    @Test fun `upgrade from version one preserves learned progress and adds nullable JLPT`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val name = "migration-test.db"
        context.deleteDatabase(name)
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(context).name(name)
                .callback(object : SupportSQLiteOpenHelper.Callback(1) {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        db.execSQL("CREATE TABLE kanji_table (kanji TEXT NOT NULL PRIMARY KEY, kunReadings TEXT NOT NULL, onReadings TEXT NOT NULL, meanings TEXT NOT NULL, isLearned INTEGER NOT NULL)")
                        db.execSQL("INSERT INTO kanji_table VALUES (?, ?, ?, ?, ?)", arrayOf("日", "[\"ひ\"]", "[\"ニチ\"]", "[\"sun\",\"day\"]", 1))
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) = Unit
                }).build()
        )
        helper.writableDatabase
        helper.close()
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3).build()
        try {
            // Opening with Room also validates the migrated table against the current entity.
            assertEquals(listOf(entity(learned = true).copy(jlpt = null)), database.kanjiDao().getAllKanjis())
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
    @Test fun `version two migration treats a complete catalog as authoritative even with no learned entries`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val name = "migration-v2-test.db"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, 0, null).use { legacy ->
            legacy.execSQL("CREATE TABLE kanji_table (kanji TEXT NOT NULL PRIMARY KEY, kunReadings TEXT NOT NULL, onReadings TEXT NOT NULL, meanings TEXT NOT NULL, isLearned INTEGER NOT NULL, jlpt INTEGER)")
            legacy.beginTransaction()
            try {
                repeat(2136) { index ->
                    legacy.execSQL("INSERT INTO kanji_table VALUES (?, '[]', '[]', '[]', 0, NULL)", arrayOf((0x4E00 + index).toChar().toString()))
                }
                legacy.setTransactionSuccessful()
            } finally {
                legacy.endTransaction()
            }
            legacy.version = 2
        }
        val database = Room.databaseBuilder(context, AppDatabase::class.java, name)
            .addMigrations(AppDatabase.MIGRATION_2_3).build()
        try {
            assertEquals(2136, database.kanjiDao().countKanjis())
            org.junit.Assert.assertTrue(database.kanjiDao().isProgressInitialized())
            org.junit.Assert.assertTrue(database.kanjiDao().getLearnedKanjisOnce().isEmpty())
        } finally {
            database.close()
            context.deleteDatabase(name)
        }
    }
}
