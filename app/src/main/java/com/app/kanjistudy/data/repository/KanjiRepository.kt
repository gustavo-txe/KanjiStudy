package com.app.kanjistudy.data.repository

import com.app.kanjistudy.data.backup.KanjiAutoBackup
import com.app.kanjistudy.data.local.AppDatabase
import com.app.kanjistudy.data.local.KanjiDao
import com.app.kanjistudy.data.local.KanjiEntity
import com.app.kanjistudy.data.mapper.toDomain
import com.app.kanjistudy.data.mapper.toEntity
import com.app.kanjistudy.data.preferences.AppPreferencesRepository
import com.app.kanjistudy.data.remote.KanjiApiService
import com.app.kanjistudy.domain.model.Kanji
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonParser
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KanjiRepository @Inject constructor(
    private val kanjiDao: KanjiDao,
    private val api: KanjiApiService,
    private val preferencesRepository: AppPreferencesRepository,
    private val kanjiAutoBackup: KanjiAutoBackup
) {

    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
    private val learnedKanjiMutex = Mutex()
    private val gson = Gson()
    private val prettyGson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private companion object {
        const val MIN_KANJI_COUNT = 2136
        const val CHUNK_SIZE = 40
        const val SQLITE_BIND_PARAMETER_LIMIT = 500
        const val EXPORT_SCHEMA_VERSION = 1
        const val MAX_RETRIES = 3
        const val RETRY_DELAY_MS = 400L
    }

    suspend fun ensureAllKanjisLoaded(
        onProgress: (Float) -> Unit
    ): List<Kanji> = withContext(ioDispatcher) {

        val count = kanjiDao.countKanjis()
        if (count >= MIN_KANJI_COUNT) {
            refreshKanjiUpdated(onProgress = onProgress)
            restoreLearnedKanjisFromAutoBackup()
            return@withContext kanjiDao.getAllKanjis().map { it.toDomain() }
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
        kanjiDao.getAllKanjis().map { it.toDomain() }
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
                        fetchReadingMeaningWithRetry(kanji)?.let { kanjiEntity ->
                            kanjiEntity.copy(
                                isLearned = learnedKanjis[kanjiEntity.kanji]?.isLearned == true
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

    private suspend fun shouldRefreshSchema(): Boolean {
        return preferencesRepository.shouldRefreshSchema(AppDatabase.DATABASE_VERSION)
    }

    private suspend fun markSchemaAsRefreshed() {
        preferencesRepository.markSchemaAsRefreshed(AppDatabase.DATABASE_VERSION)
    }

    suspend fun toggleLearnedKanji(kanji: String) = withContext(ioDispatcher) {
        learnedKanjiMutex.withLock {
            kanjiDao.toggleLearned(kanji)
            syncLearnedKanjisToAutoBackupLocked()
        }
    }

    suspend fun exportLearnedKanjisJson(): String = withContext(ioDispatcher) {
        buildLearnedKanjisJson()
    }

    suspend fun importLearnedKanjisJson(json: String): LearnedKanjiImportResult =
        withContext(ioDispatcher) {
            val importedKanjis = normalizeLearnedKanjis(parseLearnedKanjiJson(json))

            if (importedKanjis.isEmpty()) {
                return@withContext LearnedKanjiImportResult(importedCount = 0, skippedCount = 0)
            }

            learnedKanjiMutex.withLock {
                val result = importLearnedKanjiList(importedKanjis)
                syncLearnedKanjisToAutoBackupLocked()
                result
            }
        }

    private suspend fun restoreLearnedKanjisFromAutoBackup() {
        learnedKanjiMutex.withLock {
            val backupJson = runCatching { kanjiAutoBackup.readBackupJson() }
                .getOrNull()

            if (backupJson == null) {
                syncLearnedKanjisToAutoBackupLocked()
                return
            }

            val backupKanjis = runCatching {
                normalizeLearnedKanjis(parseLearnedKanjiJson(backupJson))
            }.getOrNull()

            if (!backupKanjis.isNullOrEmpty()) {
                importLearnedKanjiList(backupKanjis)
            }

            syncLearnedKanjisToAutoBackupLocked()
        }
    }

    private suspend fun importLearnedKanjiList(importedKanjis: List<String>): LearnedKanjiImportResult {
        if (importedKanjis.isEmpty()) {
            return LearnedKanjiImportResult(importedCount = 0, skippedCount = 0)
        }

        val existingKanjis = importedKanjis
            .chunked(SQLITE_BIND_PARAMETER_LIMIT)
            .flatMap { chunk -> kanjiDao.getExistingKanjiChars(chunk) }
            .toSet()
        val validKanjis = importedKanjis.filter { it in existingKanjis }
        val importedCount = kanjiDao.importLearnedKanjis(validKanjis)

        return LearnedKanjiImportResult(
            importedCount = importedCount,
            skippedCount = importedKanjis.size - validKanjis.size
        )
    }

    private suspend fun syncLearnedKanjisToAutoBackupLocked() {
        val backupJson = buildLearnedKanjisJson()
        runCatching { kanjiAutoBackup.writeBackupJson(backupJson) }
    }

    private suspend fun buildLearnedKanjisJson(): String {
        val learnedKanjis = kanjiDao.getLearnedKanjisOnce()
            .map { it.kanji }
            .sorted()

        return prettyGson.toJson(
            LearnedKanjiExport(
                version = EXPORT_SCHEMA_VERSION,
                learnedKanji = learnedKanjis
            )
        )
    }

    private fun normalizeLearnedKanjis(kanjis: List<String>): List<String> {
        return kanjis
            .asSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .toList()
    }

    private fun parseLearnedKanjiJson(json: String): List<String> {
        val root = runCatching { JsonParser.parseString(json) }
            .getOrElse { throw IllegalArgumentException("Invalid JSON file.") }

        return when {
            root.isJsonArray -> gson.fromJson(root, Array<String>::class.java).toList()
            root.isJsonObject -> {
                val jsonObject = root.asJsonObject
                val kanjiElement = jsonObject.get("learnedKanji") ?: jsonObject.get("learnedKanjis")
                if (kanjiElement == null || !kanjiElement.isJsonArray) {
                    throw IllegalArgumentException("The JSON file must include a learnedKanji array.")
                }
                gson.fromJson(kanjiElement, Array<String>::class.java).toList()
            }

            else -> throw IllegalArgumentException("The JSON file must include a learnedKanji array.")
        }
    }

    fun getLearnedKanjis(): Flow<List<Kanji>> =
        kanjiDao.getLearnedKanjis().map { kanjis -> kanjis.map { it.toDomain() } }

    suspend fun getKanjisByChars(kanjis: Set<Char>): List<Kanji> = withContext(ioDispatcher) {
        if (kanjis.isEmpty()) return@withContext emptyList()
        kanjiDao.getKanjisByChars(kanjis.map { it.toString() }).map { it.toDomain() }
    }

    suspend fun isKanjiDownloadComplete(): Boolean = withContext(ioDispatcher) {
        kanjiDao.countKanjis() >= MIN_KANJI_COUNT
    }

    private suspend fun fetchReadingMeaningWithRetry(kanji: String): KanjiEntity? {
        repeat(MAX_RETRIES) { attempt ->
            runCatching {
                return api.getReadingMeaning(kanji).toEntity()
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
