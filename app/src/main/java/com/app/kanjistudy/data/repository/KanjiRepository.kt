package com.app.kanjistudy.data.repository

import com.app.kanjistudy.data.backup.KanjiAutoBackup
import com.app.kanjistudy.data.local.KanjiDao
import com.app.kanjistudy.data.local.KanjiEntity
import com.app.kanjistudy.data.mapper.toDomain
import com.app.kanjistudy.data.mapper.toEntity
import com.app.kanjistudy.data.preferences.AppPreferencesRepository
import com.app.kanjistudy.data.remote.KanjiApiService
import com.app.kanjistudy.di.IoDispatcher
import com.app.kanjistudy.domain.model.Kanji
import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class KanjiRepository @Inject constructor(
    private val kanjiDao: KanjiDao,
    private val api: KanjiApiService,
    private val preferencesRepository: AppPreferencesRepository,
    private val kanjiAutoBackup: KanjiAutoBackup,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) {

    private val learnedKanjiMutex = Mutex()
    private val _isAutoBackupPending = MutableStateFlow(false)
    val isAutoBackupPending = _isAutoBackupPending.asStateFlow()
    private val prettyGson = GsonBuilder()
        .setPrettyPrinting()
        .create()

    private companion object {
        const val CATALOG_VERSION = 2
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
            try {
                refreshKanjiUpdated(onProgress = onProgress)
            } catch (exception: IOException) {
                // A failed network refresh must not hide the downloaded catalog.
            } catch (exception: HttpException) {
                // Keep the refresh pending so a later load can try again.
            }
            restoreLearnedKanjisFromAutoBackup()
            return@withContext kanjiDao.getAllKanjis().map { it.toDomain() }
        }

        val joyoKanjis = api.getJoyoKanjis()
        val total = joyoKanjis.size
        check(total >= MIN_KANJI_COUNT) {
            "The kanji API returned an incomplete Joyo kanji list."
        }
        var processed = 0

        joyoKanjis.chunked(CHUNK_SIZE).forEach { chunk ->
            val kanjiData = coroutineScope {
                chunk.map { kanji ->
                    async {
                        fetchReadingMeaningWithRetry(kanji)
                    }
                }.awaitAll()
            }

            kanjiDao.insertAll(kanjiData)

            processed += chunk.size
            onProgress(processed.toFloat() / total)
        }

        check(kanjiDao.countKanjis() >= MIN_KANJI_COUNT) {
            "The kanji download completed with missing entries."
        }

        markSchemaAsRefreshed()
        restoreLearnedKanjisFromAutoBackup()
        kanjiDao.getAllKanjis().map { it.toDomain() }
    }

    private suspend fun refreshKanjiUpdated(onProgress: (Float) -> Unit) {
        if (!shouldRefreshSchema()) {
            return
        }

        val joyoKanjis = api.getJoyoKanjis()
        val total = joyoKanjis.size
        var processed = 0
        check(total >= MIN_KANJI_COUNT) {
            "The kanji API returned an incomplete Joyo kanji list."
        }

        joyoKanjis.chunked(CHUNK_SIZE).forEach { chunk ->
            val refreshedKanjis = coroutineScope {
                chunk.map { kanji ->
                    async {
                        fetchReadingMeaningWithRetry(kanji)
                    }
                }.awaitAll()
            }

            kanjiDao.refreshMetadata(refreshedKanjis)

            processed += chunk.size
            onProgress(processed.toFloat() / total)
        }

        markSchemaAsRefreshed()
    }

    private suspend fun shouldRefreshSchema(): Boolean {
        return preferencesRepository.shouldRefreshSchema(CATALOG_VERSION)
    }

    private suspend fun markSchemaAsRefreshed() {
        preferencesRepository.markSchemaAsRefreshed(CATALOG_VERSION)
    }

    suspend fun toggleLearnedKanji(kanji: String) = withContext(ioDispatcher) {
        learnedKanjiMutex.withLock {
            ensureProgressReadyLocked()
            kanjiDao.toggleLearned(kanji)
            syncLearnedKanjisToAutoBackupLocked()
        }
    }

    suspend fun exportLearnedKanjisJson(): String = withContext(ioDispatcher) {
        learnedKanjiMutex.withLock {
            ensureProgressReadyLocked()
            buildLearnedKanjisJson()
        }
    }

    suspend fun importLearnedKanjisJson(json: String): LearnedKanjiImportResult =
        withContext(ioDispatcher) {
            val importedKanjis = normalizeLearnedKanjis(parseLearnedKanjiJson(json))

            if (importedKanjis.isEmpty()) {
                return@withContext LearnedKanjiImportResult(importedCount = 0, skippedCount = 0)
            }

            learnedKanjiMutex.withLock {
                ensureProgressReadyLocked()
                val result = importLearnedKanjiList(importedKanjis)
                syncLearnedKanjisToAutoBackupLocked()
                result
            }
        }

    private suspend fun ensureProgressReadyLocked() {
        if (kanjiDao.countKanjis() < MIN_KANJI_COUNT) {
            throw KanjiCatalogIncompleteException()
        }
        if (!kanjiDao.isProgressInitialized() && !restoreLearnedKanjisLocked()) {
            throw ProgressRestorationPendingException()
        }
    }

    private suspend fun restoreLearnedKanjisFromAutoBackup() {
        learnedKanjiMutex.withLock { restoreLearnedKanjisLocked() }
    }

    private suspend fun restoreLearnedKanjisLocked(): Boolean {
        if (kanjiDao.isProgressInitialized()) {
            syncLearnedKanjisToAutoBackupLocked()
            return true
        }
        val backupJson = try {
            kanjiAutoBackup.readBackupJson()
        } catch (exception: IOException) {
            return false
        }
        val backupKanjis = try {
            backupJson?.let { normalizeLearnedKanjis(parseLearnedKanjiJson(it)) }.orEmpty()
        } catch (exception: IllegalArgumentException) {
            return false
        }
        // Restore every saved character before allowing the backup to be replaced.
        if (getExistingKanjiChars(backupKanjis).size != backupKanjis.size) return false
        kanjiDao.restoreProgress(backupKanjis)

        syncLearnedKanjisToAutoBackupLocked()
        return true
    }

    private suspend fun importLearnedKanjiList(importedKanjis: List<String>): LearnedKanjiImportResult {
        if (importedKanjis.isEmpty()) {
            return LearnedKanjiImportResult(importedCount = 0, skippedCount = 0)
        }

        val existingKanjis = getExistingKanjiChars(importedKanjis)
        val validKanjis = importedKanjis.filter { it in existingKanjis }
        val importedCount = kanjiDao.importLearnedKanjis(validKanjis)

        return LearnedKanjiImportResult(
            importedCount = importedCount,
            skippedCount = importedKanjis.size - validKanjis.size
        )
    }

    private suspend fun getExistingKanjiChars(kanjis: List<String>): Set<String> = kanjis
        .chunked(SQLITE_BIND_PARAMETER_LIMIT)
        .flatMap { chunk -> kanjiDao.getExistingKanjiChars(chunk) }
        .toSet()

    private suspend fun syncLearnedKanjisToAutoBackupLocked() {
        try {
            kanjiAutoBackup.writeBackupJson(buildLearnedKanjisJson())
            _isAutoBackupPending.value = false
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // The database remains authoritative; retry must never repeat a toggle/import.
            _isAutoBackupPending.value = true
        }
    }

    suspend fun retryAutoBackup() = withContext(ioDispatcher) {
        learnedKanjiMutex.withLock {
            ensureProgressReadyLocked()
            syncLearnedKanjisToAutoBackupLocked()
        }
    }

    private suspend fun buildLearnedKanjisJson(): String {
        val learnedKanjis = kanjiDao.getLearnedKanjisOnce()
            .map { it.kanji }
            .sorted()

        return prettyGson.toJson(JsonObject().apply {
            addProperty("version", EXPORT_SCHEMA_VERSION)
            add("learnedKanji", JsonArray().apply { learnedKanjis.forEach { add(it) } })
        })
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

        val array = when {
            root.isJsonArray -> root.asJsonArray
            root.isJsonObject -> {
                val jsonObject = root.asJsonObject
                val kanjiElement = jsonObject.get("learnedKanji")
                    ?: jsonObject.get("learnedKanjis")
                    ?: jsonObject.get("a")
                if (kanjiElement == null || !kanjiElement.isJsonArray) {
                    throw IllegalArgumentException("The JSON file must include a learnedKanji array.")
                }
                kanjiElement.asJsonArray
            }

            else -> throw IllegalArgumentException("The JSON file must include a learnedKanji array.")
        }
        require(array.all { it.isJsonPrimitive && it.asJsonPrimitive.isString }) {
            "The learnedKanji array must contain only strings."
        }
        return array.map { it.asString }
    }

    fun observeKanjis(): Flow<List<Kanji>> =
        kanjiDao.observeKanjis().map { kanjis -> kanjis.map { it.toDomain() } }

    fun getLearnedKanjis(): Flow<List<Kanji>> =
        kanjiDao.getLearnedKanjis().map { kanjis -> kanjis.map { it.toDomain() } }

    suspend fun getKanjisByChars(kanjis: Set<Char>): List<Kanji> = withContext(ioDispatcher) {
        if (kanjis.isEmpty()) return@withContext emptyList()
        kanjiDao.getKanjisByChars(kanjis.map { it.toString() }).map { it.toDomain() }
    }

    suspend fun isKanjiDownloadComplete(): Boolean = withContext(ioDispatcher) {
        kanjiDao.countKanjis() >= MIN_KANJI_COUNT
    }

    private suspend fun fetchReadingMeaningWithRetry(kanji: String): KanjiEntity {
        var lastFailure: Throwable? = null

        repeat(MAX_RETRIES) { attempt ->
            try {
                return api.getReadingMeaning(kanji).toEntity()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                lastFailure = exception
            }

            if (attempt < MAX_RETRIES - 1) {
                delay((RETRY_DELAY_MS * (attempt + 1)).milliseconds)
            }
        }

        throw KanjiDownloadException(kanji, lastFailure)
    }

}

data class LearnedKanjiImportResult(
    val importedCount: Int,
    val skippedCount: Int
)

private class KanjiDownloadException(
    kanji: String,
    cause: Throwable?
) : IOException("Unable to download data for kanji $kanji.", cause)
