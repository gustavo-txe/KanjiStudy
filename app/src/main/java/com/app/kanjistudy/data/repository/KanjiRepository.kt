package com.app.kanjistudy.data.repository

import android.content.SharedPreferences
import androidx.core.content.edit
import com.app.kanjistudy.data.backup.KanjiAutoBackup
import com.app.kanjistudy.data.local.AppDatabase
import com.app.kanjistudy.data.model.KanjiData
import com.app.kanjistudy.data.local.KanjiDao
import com.app.kanjistudy.data.remote.KanjiApiService
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
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
    private val sharedPreferences: SharedPreferences,
    private val kanjiAutoBackup: KanjiAutoBackup
) {

    private companion object {
        const val MIN_KANJI_COUNT = 2136
        const val CHUNK_SIZE = 40
        const val EXPORT_SCHEMA_VERSION = 1
        const val MAX_RETRIES = 3
        const val RETRY_DELAY_MS = 400L
        const val LAST_REFRESHED_SCHEMA_VERSION = "last_refreshed_schema_version"
    }

    suspend fun ensureAllKanjisLoaded(
        onProgress: (Float) -> Unit
    ): List<KanjiData> = withContext(Dispatchers.IO) {

        val count = kanjiDao.countKanjis()
        if (count >= MIN_KANJI_COUNT) {
            refreshKanjiUpdated(onProgress = onProgress)
            restoreLearnedKanjisFromAutoBackup()
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

        markSchemaAsRefreshed()
        restoreLearnedKanjisFromAutoBackup()
        kanjiDao.getAllKanjis()
    }

    private suspend fun refreshKanjiUpdated(onProgress: (Float) -> Unit) {

            if (!shouldRefreshSchema()) {
                return
            }

            val learnedKanjis = kanjiDao.getLearnedKanjisOnce()
                .associateBy { it.kanji }

            val joyoKanjis = api.getJoyoKanjis()
            val total = joyoKanjis.size
            var processed = 0

            joyoKanjis.chunked(CHUNK_SIZE).forEach { chunk ->
                val refreshedKanjis = coroutineScope {
                    chunk.map { kanji ->
                        async {
                            fetchReadingMeaningWithRetry(kanji)?.let { apiKanji ->
                                apiKanji.copy(
                                    isLearned = learnedKanjis[apiKanji.kanji]?.isLearned == true
                                )
                            }
                        }
                    }.awaitAll().filterNotNull()
                }

                kanjiDao.upsertAll(refreshedKanjis)

                processed += chunk.size
                onProgress(processed.toFloat() / total)
            }

            markSchemaAsRefreshed()
        }

        private fun shouldRefreshSchema(): Boolean {
            val lastRefreshedSchemaVersion =
                sharedPreferences.getInt(LAST_REFRESHED_SCHEMA_VERSION, 0)
            return lastRefreshedSchemaVersion < AppDatabase.DATABASE_VERSION
        }

        private fun markSchemaAsRefreshed() {
            sharedPreferences.edit {
                putInt(LAST_REFRESHED_SCHEMA_VERSION, AppDatabase.DATABASE_VERSION)
            }
        }

    suspend fun toggleLearnedKanji(kanji: String) = withContext(Dispatchers.IO) {
        val isLearned = kanjiDao.isKanjiLearned(kanji)
        if (isLearned) kanjiDao.uncheckLearnedKanji(kanji)
        else kanjiDao.markAsLearned(kanji)
        syncLearnedKanjisToAutoBackup()
    }

    suspend fun exportLearnedKanjisJson(): String = withContext(Dispatchers.IO) {
        buildLearnedKanjisJson()
    }

    suspend fun importLearnedKanjisJson(json: String): LearnedKanjiImportResult = withContext(Dispatchers.IO) {
        val importedKanjis = parseLearnedKanjiJson(json)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()

        if (importedKanjis.isEmpty()) {
            return@withContext LearnedKanjiImportResult(importedCount = 0, skippedCount = 0)
        }

        val result = importLearnedKanjiList(importedKanjis)
        syncLearnedKanjisToAutoBackup()
        result
    }

    private suspend fun restoreLearnedKanjisFromAutoBackup() {
        val backupJson = runCatching { kanjiAutoBackup.readBackupJson() }
            .getOrNull()

        if (backupJson == null) {
            syncLearnedKanjisToAutoBackup()
            return
        }

        val backupKanjis = runCatching {
            parseLearnedKanjiJson(backupJson)
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        }.getOrNull()

        if (!backupKanjis.isNullOrEmpty()) {
            importLearnedKanjiList(backupKanjis)
        }

        syncLearnedKanjisToAutoBackup()
    }

    private suspend fun importLearnedKanjiList(importedKanjis: List<String>): LearnedKanjiImportResult {
        if (importedKanjis.isEmpty()) {
            return LearnedKanjiImportResult(importedCount = 0, skippedCount = 0)
        }

        val existingKanjis = kanjiDao.getExistingKanjiChars(importedKanjis).toSet()
        val validKanjis = importedKanjis.filter { it in existingKanjis }
        val importedCount = kanjiDao.importLearnedKanjis(validKanjis)

        return LearnedKanjiImportResult(
            importedCount = importedCount,
            skippedCount = importedKanjis.size - validKanjis.size
        )
    }

    private suspend fun syncLearnedKanjisToAutoBackup() {
        val backupJson = buildLearnedKanjisJson()
        runCatching { kanjiAutoBackup.writeBackupJson(backupJson) }
    }

    private suspend fun buildLearnedKanjisJson(): String {
        val learnedKanjis = kanjiDao.getLearnedKanjisOnce()
            .map { it.kanji }
            .sorted()

        return GsonBuilder()
            .setPrettyPrinting()
            .create()
            .toJson(
                LearnedKanjiExport(
                    version = EXPORT_SCHEMA_VERSION,
                    learnedKanji = learnedKanjis
                )
            )
    }


    private fun parseLearnedKanjiJson(json: String): List<String> {
        val root = runCatching { JsonParser.parseString(json) }
            .getOrElse { throw IllegalArgumentException("Invalid JSON file.") }

        return when {
            root.isJsonArray -> Gson().fromJson(root, Array<String>::class.java).toList()
            root.isJsonObject -> {
                val jsonObject = root.asJsonObject
                val kanjiElement = jsonObject.get("learnedKanji") ?: jsonObject.get("learnedKanjis")
                if (kanjiElement == null || !kanjiElement.isJsonArray) {
                    throw IllegalArgumentException("The JSON file must include a learnedKanji array.")
                }
                Gson().fromJson(kanjiElement, Array<String>::class.java).toList()
            }
            else -> throw IllegalArgumentException("The JSON file must include a learnedKanji array.")
        }
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

data class LearnedKanjiImportResult(
    val importedCount: Int,
    val skippedCount: Int
)

private data class LearnedKanjiExport(
    val version: Int,
    val learnedKanji: List<String>
)
