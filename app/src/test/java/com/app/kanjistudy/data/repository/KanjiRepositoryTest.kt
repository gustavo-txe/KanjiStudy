package com.app.kanjistudy.data.repository

import app.cash.turbine.test
import com.app.kanjistudy.data.backup.KanjiAutoBackup
import com.app.kanjistudy.data.local.KanjiDao
import com.app.kanjistudy.data.preferences.AppPreferencesRepository
import com.app.kanjistudy.data.remote.KanjiApiService
import com.app.kanjistudy.data.remote.KanjiDto
import com.app.kanjistudy.testing.entity
import com.google.gson.JsonParser
import io.mockk.*
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class KanjiRepositoryTest {
    @Test fun `failed backup is observable and retry writes current progress without repeating toggle`() = runTest {
        val repo = repository(StandardTestDispatcher(testScheduler))
        coEvery { dao.toggleLearned("日") } returns 1
        coEvery { dao.getLearnedKanjisOnce() } returns listOf(entity(learned = true))
        coEvery { backup.writeBackupJson(any()) } throws IOException("disk full")
        repo.toggleLearnedKanji("日")
        assertTrue(repo.isAutoBackupPending.value)
        repo.retryAutoBackup()
        assertTrue(repo.isAutoBackupPending.value)
        coEvery { backup.writeBackupJson(any()) } returns Unit
        repo.retryAutoBackup()
        assertFalse(repo.isAutoBackupPending.value)
        coVerify(exactly = 1) { dao.toggleLearned("日") }
        coVerify(exactly = 3) { backup.writeBackupJson(match { it.contains("日") }) }
    }

    @Test fun `backup cancellation propagates instead of becoming a recoverable write failure`() = runTest {
        val repo = repository(StandardTestDispatcher(testScheduler))
        coEvery { dao.getLearnedKanjisOnce() } returns emptyList()
        coEvery { backup.writeBackupJson(any()) } throws CancellationException("cancelled")
        try {
            repo.retryAutoBackup()
            fail("Expected cancellation")
        } catch (_: CancellationException) {
            assertFalse(repo.isAutoBackupPending.value)
        }
    }
    private val dao = mockk<KanjiDao>()
    private val api = mockk<KanjiApiService>()
    private val preferences = mockk<AppPreferencesRepository>()
    private val backup = mockk<KanjiAutoBackup>()

    init {
        coEvery { dao.countKanjis() } returns 2136
        coEvery { dao.isProgressInitialized() } returns true
        coEvery { dao.restoreProgress(any()) } returns Unit
    }

    private fun repository(dispatcher: kotlinx.coroutines.CoroutineDispatcher) =
        KanjiRepository(dao, api, preferences, backup, dispatcher)

    @Test fun `import supports current legacy and plain array backups and normalizes entries`() = runTest {
        val repo = repository(StandardTestDispatcher(testScheduler))
        coEvery { dao.getExistingKanjiChars(listOf("日", "月", "外")) } returns listOf("日", "月")
        coEvery { dao.importLearnedKanjis(listOf("日", "月")) } returns 2
        coEvery { dao.getLearnedKanjisOnce() } returns listOf(entity(learned = true))
        coEvery { backup.writeBackupJson(any()) } returns Unit
        val entries = """[" 日 ", "月", "日", "", "  ", "外"]"""
        for (json in listOf(entries, """{"version":1,"learnedKanji":$entries}""", """{"learnedKanjis":$entries}""", """{"a":$entries}""")) {
            assertEquals(LearnedKanjiImportResult(2, 1), repo.importLearnedKanjisJson(json))
        }
        coVerify(exactly = 4) { dao.importLearnedKanjis(listOf("日", "月")) }
    }

    @Test fun `invalid backup entries are rejected before any database change`() = runTest {
        val repo = repository(StandardTestDispatcher(testScheduler))
        for (json in listOf("{", "null", "{}", "42", """{"learnedKanji":"日"}""", "[null]", "[1]", "[true]", "[{}]",
            """{"a":null}""", """{"a":"日"}""", """{"a":["日",null]}""",
            """{"a":[1]}""", """{"a":[true]}""", """{"a":[{}]}""",
            """{"learnedKanji":null,"a":["日"]}""", """{"b":["日"]}""")) {
            try {
                repo.importLearnedKanjisJson(json)
                fail("Expected invalid backup to be rejected: $json")
            } catch (_: IllegalArgumentException) {
                // Public import contract: invalid content is a validation error.
            }
        }
        coVerify(exactly = 0) { dao.importLearnedKanjis(any()) }
        coVerify(exactly = 0) { backup.writeBackupJson(any()) }
    }

    @Test fun `empty import is a no-op`() = runTest {
        for (json in listOf("[]", """{"a":[]}""", """{"a":["", " "]}""")) {
            assertEquals(LearnedKanjiImportResult(0, 0), repository(StandardTestDispatcher(testScheduler)).importLearnedKanjisJson(json))
        }
        coVerify(exactly = 0) { dao.importLearnedKanjis(any()) }
        coVerify(exactly = 0) { backup.writeBackupJson(any()) }
    }

    @Test fun `explicit backup fields take precedence over the Play Store alias`() = runTest {
        val repo = repository(StandardTestDispatcher(testScheduler))
        assertEquals(LearnedKanjiImportResult(0, 0), repo.importLearnedKanjisJson("""{"learnedKanji":[],"a":["日"]}"""))
        assertEquals(LearnedKanjiImportResult(0, 0), repo.importLearnedKanjisJson("""{"learnedKanjis":[],"a":["日"]}"""))
        coVerify(exactly = 0) { dao.importLearnedKanjis(any()) }
    }

    @Test fun `large imports split existence lookups below SQLite parameter limit`() = runTest {
        val chars = (0x4E00 until 0x4E00 + 1001).map { it.toChar().toString() }
        val chunks = mutableListOf<List<String>>()
        coEvery { dao.getExistingKanjiChars(capture(chunks)) } answers { firstArg() }
        coEvery { dao.importLearnedKanjis(chars) } returns chars.size
        coEvery { dao.getLearnedKanjisOnce() } returns emptyList()
        coEvery { backup.writeBackupJson(any()) } returns Unit
        val json = com.google.gson.Gson().toJson(chars)
        assertEquals(LearnedKanjiImportResult(1001, 0), repository(StandardTestDispatcher(testScheduler)).importLearnedKanjisJson(json))
        assertEquals(listOf(500, 500, 1), chunks.map { it.size })
        assertEquals(chars, chunks.flatten())
    }

    @Test fun `export is versioned sorted and contains learned characters only`() = runTest {
        coEvery { dao.getLearnedKanjisOnce() } returns listOf(entity("月", true), entity("日", true))
        val json = JsonParser.parseString(repository(StandardTestDispatcher(testScheduler)).exportLearnedKanjisJson()).asJsonObject
        assertEquals(1, json["version"].asInt)
        assertEquals(listOf("日", "月"), json["learnedKanji"].asJsonArray.map { it.asString })
    }

    @Test fun `toggle writes backup after persistence and tolerates backup IO failure`() = runTest {
        coEvery { dao.toggleLearned("日") } returns 1
        coEvery { dao.getLearnedKanjisOnce() } returns listOf(entity(learned = true))
        coEvery { backup.writeBackupJson(any()) } throws IOException("disk full")
        repository(StandardTestDispatcher(testScheduler)).toggleLearnedKanji("日")
        coVerifyOrder {
            dao.toggleLearned("日")
            dao.getLearnedKanjisOnce()
            backup.writeBackupJson(any())
        }
    }

    @Test fun `backup cancellation is propagated`() = runTest {
        coEvery { dao.toggleLearned("日") } returns 1
        coEvery { dao.getLearnedKanjisOnce() } returns emptyList()
        val cancellation = CancellationException("cancelled")
        coEvery { backup.writeBackupJson(any()) } throws cancellation
        try {
            repository(StandardTestDispatcher(testScheduler)).toggleLearnedKanji("日")
            fail("Cancellation must propagate")
        } catch (actual: CancellationException) {
            assertEquals(cancellation.message, actual.message)
        }
    }

    @Test fun `complete current database loads offline and restores learned backup`() = runTest {
        coEvery { dao.isProgressInitialized() } returns false
        coEvery { dao.countKanjis() } returns 2136
        coEvery { preferences.shouldRefreshSchema(2) } returns false
        coEvery { backup.readBackupJson() } returns """["日"]"""
        coEvery { dao.getExistingKanjiChars(listOf("日")) } returns listOf("日")
        coEvery { dao.importLearnedKanjis(listOf("日")) } returns 1
        coEvery { dao.getLearnedKanjisOnce() } returns listOf(entity(learned = true))
        coEvery { dao.getAllKanjis() } returns listOf(entity(learned = true))
        coEvery { backup.writeBackupJson(any()) } returns Unit
        val result = repository(StandardTestDispatcher(testScheduler)).ensureAllKanjisLoaded { fail("No download needed") }
        assertTrue(result.single().isLearned)
        coVerify(exactly = 0) { api.getJoyoKanjis() }
    }

    @Test fun `incomplete API list is rejected on initial load and schema refresh`() = runTest {
        coEvery { api.getJoyoKanjis() } returns listOf("日")
        coEvery { preferences.shouldRefreshSchema(2) } returns true
        coEvery { dao.getLearnedKanjisOnce() } returns emptyList()
        for (count in listOf(0, 2136)) {
            coEvery { dao.countKanjis() } returns count
            try {
                repository(StandardTestDispatcher(testScheduler)).ensureAllKanjisLoaded {}
                fail("Incomplete API response accepted")
            } catch (_: IllegalStateException) { }
        }
        coVerify(exactly = 0) { preferences.markSchemaAsRefreshed(any()) }
        coVerify(exactly = 0) { api.getReadingMeaning(any()) }
    }

    @Test fun `download retries transient errors with virtual backoff and saves all entries`() = runTest {
        coEvery { dao.isProgressInitialized() } returns false
        val chars = (0x4E00 until 0x4E00 + 2136).map { it.toChar().toString() }
        coEvery { dao.countKanjis() } returnsMany listOf(0, 2136)
        coEvery { api.getJoyoKanjis() } returns chars
        coEvery { api.getReadingMeaning(any()) } answers { KanjiDto(firstArg(), 5, emptyList(), emptyList(), listOf("meaning")) }
        var attempts = 0
        coEvery { api.getReadingMeaning(chars.first()) } answers {
            if (++attempts < 3) throw IOException("temporary")
            KanjiDto(chars.first(), 5, emptyList(), emptyList(), listOf("meaning"))
        }
        val saved = mutableListOf<List<com.app.kanjistudy.data.local.KanjiEntity>>()
        coEvery { dao.insertAll(capture(saved)) } returns Unit
        coEvery { preferences.markSchemaAsRefreshed(2) } returns Unit
        coEvery { backup.readBackupJson() } returns null
        coEvery { dao.getLearnedKanjisOnce() } returns emptyList()
        coEvery { backup.writeBackupJson(any()) } returns Unit
        coEvery { dao.getAllKanjis() } returns listOf(entity())
        val progress = mutableListOf<Float>()
        repository(StandardTestDispatcher(testScheduler)).ensureAllKanjisLoaded(progress::add)
        assertEquals(3, attempts)
        assertEquals(1200L, testScheduler.currentTime)
        assertEquals(chars, saved.flatten().map { it.kanji })
        assertTrue(saved.all { it.size <= 40 })
        assertEquals(1f, progress.last())
        assertTrue(progress.zipWithNext().all { (a, b) -> b > a })
    }

    @Test fun `learned flow maps entity fields and subsequent changes`() = runTest {
        val entities = MutableStateFlow(listOf(entity(learned = true)))
        every { dao.getLearnedKanjis() } returns entities
        repository(StandardTestDispatcher(testScheduler)).getLearnedKanjis().test {
            val result = awaitItem().single()
            assertEquals("日", result.kanji)
            assertEquals(5, result.jlpt)
            assertEquals(listOf("ひ"), result.kunReadings)
            assertTrue(result.isLearned)
            entities.value = emptyList()
            assertTrue(awaitItem().isEmpty())
        }
    }

    @Test fun `download readiness uses minimum count and empty lookup skips database`() = runTest {
        val repo = repository(StandardTestDispatcher(testScheduler))
        for (count in listOf(2135, 2136, 2137)) {
            coEvery { dao.countKanjis() } returns count
            assertEquals(count >= 2136, repo.isKanjiDownloadComplete())
        }
        assertTrue(repo.getKanjisByChars(emptySet()).isEmpty())
        coVerify(exactly = 0) { dao.getKanjisByChars(any()) }
    }

    @Test fun `persistent download failure stops after three attempts without completing schema`() = runTest {
        val chars = (0x4E00 until 0x4E00 + 2136).map { it.toChar().toString() }
        coEvery { dao.countKanjis() } returns 0
        coEvery { api.getJoyoKanjis() } returns chars
        coEvery { api.getReadingMeaning(any()) } answers { KanjiDto(firstArg(), 5, emptyList(), emptyList(), emptyList()) }
        coEvery { api.getReadingMeaning(chars.first()) } throws IOException("offline")
        try {
            repository(StandardTestDispatcher(testScheduler)).ensureAllKanjisLoaded {}
            fail("Download should fail")
        } catch (error: Exception) {
            assertTrue(error.message.orEmpty().contains(chars.first()))
            assertTrue(error.cause is IOException)
        }
        coVerify(exactly = 3) { api.getReadingMeaning(chars.first()) }
        coVerify(exactly = 0) { dao.insertAll(any()) }
        coVerify(exactly = 0) { preferences.markSchemaAsRefreshed(any()) }
        assertEquals(1200L, testScheduler.currentTime)
    }

    @Test fun `download cancellation is never retried`() = runTest {
        coEvery { dao.countKanjis() } returns 0
        coEvery { api.getJoyoKanjis() } returns (0x4E00 until 0x4E00 + 2136).map { it.toChar().toString() }
        coEvery { api.getReadingMeaning(any()) } throws CancellationException("cancelled")
        try {
            repository(StandardTestDispatcher(testScheduler)).ensureAllKanjisLoaded {}
            fail("Cancellation must propagate")
        } catch (error: CancellationException) {
            assertEquals("cancelled", error.message)
        }
        coVerify(exactly = 1) { api.getReadingMeaning("一") }
        coVerify(exactly = 0) { dao.insertAll(any()) }
        coVerify(exactly = 0) { preferences.markSchemaAsRefreshed(any()) }
        assertEquals(0L, testScheduler.currentTime)
    }

    @Test fun `schema refresh delegates metadata updates and returns current database state`() = runTest {
        val chars = (0x4E00 until 0x4E00 + 2136).map { it.toChar().toString() }
        coEvery { dao.countKanjis() } returns 2136
        coEvery { preferences.shouldRefreshSchema(2) } returns true
        coEvery { dao.getLearnedKanjisOnce() } returns listOf(entity(chars.first(), true))
        coEvery { api.getJoyoKanjis() } returns chars
        coEvery { api.getReadingMeaning(any()) } answers { KanjiDto(firstArg(), 4, listOf("new"), emptyList(), listOf("updated")) }
        val batches = mutableListOf<List<com.app.kanjistudy.data.local.KanjiEntity>>()
        coEvery { dao.refreshMetadata(capture(batches)) } returns Unit
        coEvery { preferences.markSchemaAsRefreshed(2) } returns Unit
        coEvery { backup.readBackupJson() } returns null
        coEvery { backup.writeBackupJson(any()) } returns Unit
        coEvery { dao.getAllKanjis() } answers {
            batches.flatten().map { it.copy(isLearned = it.kanji == chars.first()) }
        }
        val result = repository(StandardTestDispatcher(testScheduler)).ensureAllKanjisLoaded {}
        assertEquals(chars, result.map { it.kanji })
        assertEquals(listOf(chars.first()), result.filter { it.isLearned }.map { it.kanji })
        assertTrue(result.all { it.jlpt == 4 && it.meanings == listOf("updated") })
        coVerify(exactly = 1) { preferences.markSchemaAsRefreshed(2) }
    }

    @Test fun `failed refresh returns local catalog without marking refresh complete`() = runTest {
        coEvery { dao.countKanjis() } returns 2136
        coEvery { preferences.shouldRefreshSchema(2) } returns true
        coEvery { api.getJoyoKanjis() } throws IOException("offline")
        coEvery { backup.readBackupJson() } returns null
        coEvery { dao.getLearnedKanjisOnce() } returns emptyList()
        coEvery { backup.writeBackupJson(any()) } returns Unit
        coEvery { dao.getAllKanjis() } returns listOf(entity())
        val result = repository(StandardTestDispatcher(testScheduler)).ensureAllKanjisLoaded {}
        assertEquals("日", result.single().kanji)
        coVerify(exactly = 0) { preferences.markSchemaAsRefreshed(any()) }
    }

    @Test fun `cancelled refresh propagates instead of returning local catalog`() = runTest {
        coEvery { dao.countKanjis() } returns 2136
        coEvery { preferences.shouldRefreshSchema(2) } returns true
        coEvery { api.getJoyoKanjis() } throws CancellationException("cancel refresh")
        try {
            repository(StandardTestDispatcher(testScheduler)).ensureAllKanjisLoaded {}
            fail("Cancellation must propagate")
        } catch (_: CancellationException) { }
        coVerify(exactly = 0) { dao.getAllKanjis() }
    }

    @Test fun `failed detail refresh retries then falls back to catalog and remains pending`() = runTest {
        val chars = (0x4E00 until 0x4E00 + 2136).map { it.toChar().toString() }
        coEvery { dao.countKanjis() } returns 2136
        coEvery { preferences.shouldRefreshSchema(2) } returns true
        coEvery { api.getJoyoKanjis() } returns chars
        coEvery { api.getReadingMeaning(any()) } throws IOException("offline")
        coEvery { backup.readBackupJson() } returns null
        coEvery { dao.getLearnedKanjisOnce() } returns emptyList()
        coEvery { backup.writeBackupJson(any()) } returns Unit
        coEvery { dao.getAllKanjis() } returns listOf(entity())
        val result = repository(StandardTestDispatcher(testScheduler)).ensureAllKanjisLoaded {}
        assertEquals("日", result.single().kanji)
        coVerify(exactly = 3) { api.getReadingMeaning(chars.first()) }
        coVerify(exactly = 0) { preferences.markSchemaAsRefreshed(any()) }
        coVerify(exactly = 0) { dao.refreshMetadata(any()) }
    }

    @Test fun `export can be imported using the exact public JSON field names`() = runTest {
        coEvery { dao.getLearnedKanjisOnce() } returns listOf(entity(learned = true))
        coEvery { dao.getExistingKanjiChars(listOf("日")) } returns listOf("日")
        coEvery { dao.importLearnedKanjis(listOf("日")) } returns 1
        coEvery { backup.writeBackupJson(any()) } returns Unit
        val repo = repository(StandardTestDispatcher(testScheduler))
        val json = repo.exportLearnedKanjisJson()
        assertEquals(setOf("version", "learnedKanji"), JsonParser.parseString(json).asJsonObject.keySet())
        assertEquals(LearnedKanjiImportResult(1, 0), repo.importLearnedKanjisJson(json))
    }
}
