package com.app.kanjistudy.testing

import android.content.Context
import android.content.ContextWrapper
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.room.Room
import com.app.kanjistudy.data.backup.KanjiAutoBackup
import com.app.kanjistudy.data.local.AppDatabase
import com.app.kanjistudy.data.local.KanjiEntity
import com.app.kanjistudy.data.preferences.AppPreferencesRepository
import com.app.kanjistudy.data.remote.KanjiApiService
import com.app.kanjistudy.data.remote.KanjiDto
import com.app.kanjistudy.data.repository.KanjiRepository
import java.io.Closeable
import java.io.File
import java.util.UUID
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking

/** Real storage, isolated per test; no production database, preferences or backup are touched. */
class TestStorage(context: Context) : Closeable {
    private val directory = File(context.cacheDir, "instrumented-${UUID.randomUUID()}").apply {
        check(mkdirs())
    }
    private val storeJob = SupervisorJob()
    private val store = PreferenceDataStoreFactory.create(
        scope = CoroutineScope(storeJob + Dispatchers.IO),
        produceFile = { File(directory, "settings.preferences_pb") },
    )
    val preferences = AppPreferencesRepository(store)
    val database = Room.databaseBuilder(context, AppDatabase::class.java, File(directory, "kanji.db").path).build()
    val dao = database.kanjiDao()

    private val backupContext = object : ContextWrapper(context.applicationContext) {
        override fun getApplicationContext(): Context = this
        override fun getFilesDir(): File = directory
    }
    val repository = KanjiRepository(
        dao,
        object : KanjiApiService {
            override suspend fun getJoyoKanjis(): List<String> =
                throw AssertionError("Instrumented app flows must use the seeded offline catalog")

            override suspend fun getReadingMeaning(kanji: String): KanjiDto =
                throw AssertionError("Instrumented app flows must not download kanji")
        },
        preferences,
        KanjiAutoBackup(backupContext, Dispatchers.IO),
        Dispatchers.IO,
    )

    suspend fun seedCatalog() {
        // The repository considers a download complete at 2136 entries. Only these two
        // have searchable meanings; the remaining entries satisfy that production contract.
        val entries = listOf(
            KanjiEntity("日", 5, listOf("ひ"), listOf("ニチ"), listOf("sun")),
            KanjiEntity("月", 4, listOf("つき"), listOf("ゲツ"), listOf("moon")),
        ) + (0x4E00 until 0x4E00 + 2134).map { code ->
            KanjiEntity(code.toChar().toString(), null, emptyList(), emptyList(), emptyList())
        }
        dao.insertAll(entries)
        preferences.markSchemaAsRefreshed(AppDatabase.DATABASE_VERSION)
        preferences.markHintShown("home")
        preferences.markHintShown("learned")
    }

    override fun close() {
        runBlocking { storeJob.cancelAndJoin() }
        database.close()
        check(directory.deleteRecursively())
    }
}
