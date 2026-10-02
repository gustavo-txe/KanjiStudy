package com.app.kanjistudy.data.repository

import android.app.Application
import androidx.room.Room
import com.app.kanjistudy.data.backup.KanjiAutoBackup
import com.app.kanjistudy.data.local.AppDatabase
import com.app.kanjistudy.data.preferences.AppPreferencesRepository
import com.app.kanjistudy.data.remote.KanjiApiService
import com.google.gson.JsonParser
import com.app.kanjistudy.testing.entity
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class ProgressRecoveryTest {
    private lateinit var database: AppDatabase
    private val backup = mockk<KanjiAutoBackup>()
    private val preferences = mockk<AppPreferencesRepository>()
    private val api = mockk<KanjiApiService>()

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java).build()
        coEvery { preferences.shouldRefreshSchema(2) } returns false
        coEvery { backup.readBackupJson() } returns "[\"日\"]"
        coEvery { backup.writeBackupJson(any()) } throws IOException("disk full")
    }

    @After fun tearDown() = database.close()

    private fun repository() = KanjiRepository(database.kanjiDao(), api, preferences, backup, Dispatchers.IO)

    private suspend fun seedCatalog() {
        database.kanjiDao().insertAll(
            listOf(entity()) + (0x4E00 until 0x4E00 + 2135).map { entity(it.toChar().toString()) }
        )
    }

    private fun backupFixture(name: String): String =
        checkNotNull(javaClass.getResourceAsStream("/backups/$name.json"))
            .bufferedReader(Charsets.UTF_8).use { it.readText() }

    @Test fun `published and current backup files merge with existing progress and round trip`() = runTest {
        seedCatalog()
        val dao = database.kanjiDao()
        val expected = setOf("一", "丁", "七", "日")
        dao.insertAll(expected.map { entity(it) })
        dao.toggleLearned("日")
        val repo = repository()

        for (fixture in listOf("learned-kanji-playstore", "learned-kanji-current")) {
            for (kanji in listOf("一", "丁", "七")) dao.uncheckLearnedKanji(kanji)
            assertEquals(LearnedKanjiImportResult(3, 0), repo.importLearnedKanjisJson(backupFixture(fixture)))
            assertEquals(expected, dao.getLearnedKanjisOnce().map { it.kanji }.toSet())
            repo.importLearnedKanjisJson(backupFixture(fixture))
            assertEquals(expected, dao.getLearnedKanjisOnce().map { it.kanji }.toSet())

            val exported = repo.exportLearnedKanjisJson()
            expected.forEach { dao.uncheckLearnedKanji(it) }
            assertEquals(LearnedKanjiImportResult(4, 0), repo.importLearnedKanjisJson(exported))
            assertEquals(expected, dao.getLearnedKanjisOnce().map { it.kanji }.toSet())
        }
    }

    @Test fun `automatic restoration accepts the published Play Store backup file`() = runTest {
        seedCatalog()
        coEvery { backup.readBackupJson() } returns backupFixture("learned-kanji-playstore")
        repository().ensureAllKanjisLoaded {}
        assertEquals(setOf("一", "丁", "七"), database.kanjiDao().getLearnedKanjisOnce().map { it.kanji }.toSet())
        assertTrue(database.kanjiDao().isProgressInitialized())
    }

    @Test fun `failed backup cannot restore a removal when a repository is recreated`() = runTest {
        seedCatalog()
        repository().ensureAllKanjisLoaded {}
        assertTrue(database.kanjiDao().isKanjiLearned("日"))
        repository().toggleLearnedKanji("日")
        assertFalse(database.kanjiDao().isKanjiLearned("日"))
        repository().ensureAllKanjisLoaded {}
        assertFalse(database.kanjiDao().isKanjiLearned("日"))
        coVerify(exactly = 1) { backup.readBackupJson() }
    }

    @Test fun `restoration remains pending after an unreadable backup and recovers later`() = runTest {
        seedCatalog()
        coEvery { backup.readBackupJson() } throws IOException("temporarily unavailable")
        repository().ensureAllKanjisLoaded {}
        assertFalse(database.kanjiDao().isProgressInitialized())
        coVerify(exactly = 0) { backup.writeBackupJson(any()) }
        coEvery { backup.readBackupJson() } returns "[\"日\"]"
        repository().ensureAllKanjisLoaded {}
        assertTrue(database.kanjiDao().isProgressInitialized())
        assertTrue(database.kanjiDao().isKanjiLearned("日"))
    }

    @Test fun `a user edit takes precedence over a pending old backup`() = runTest {
        seedCatalog()
        database.kanjiDao().markAsLearned("日")
        repository().toggleLearnedKanji("日")
        repository().ensureAllKanjisLoaded {}
        assertFalse(database.kanjiDao().isKanjiLearned("日"))
        coVerify(exactly = 1) { backup.readBackupJson() }
    }

    @Test fun `malformed backup is preserved without hiding the catalog`() = runTest {
        seedCatalog()
        coEvery { backup.readBackupJson() } returns "[null]"
        assertTrue(repository().ensureAllKanjisLoaded {}.isNotEmpty())
        assertFalse(database.kanjiDao().isProgressInitialized())
        coVerify(exactly = 0) { backup.writeBackupJson(any()) }
    }
    @Test fun `first edit merges pending restoration before persisting the new progress`() = runTest {
        seedCatalog()
        database.kanjiDao().insertAll(listOf(entity("月")))
        coEvery { backup.readBackupJson() } returns "[\"日\",\"月\"]"
        val additionalKanji = (0x4E00).toChar().toString()
        repository().toggleLearnedKanji(additionalKanji)
        assertTrue(database.kanjiDao().isKanjiLearned("日"))
        assertTrue(database.kanjiDao().isKanjiLearned("月"))
        assertTrue(database.kanjiDao().isKanjiLearned(additionalKanji))
    }

    @Test fun `unreadable backup blocks edits imports and exports until restoration succeeds`() = runTest {
        seedCatalog()
        val dao = database.kanjiDao()
        var savedBackup = """{"a":["日","一"]}"""
        coEvery { backup.readBackupJson() } throws IOException("temporary read failure")
        coEvery { backup.writeBackupJson(any()) } answers { savedBackup = firstArg() }
        repository().ensureAllKanjisLoaded {}

        val operations: List<suspend () -> Unit> = listOf(
            { repository().toggleLearnedKanji("丁") },
            { repository().importLearnedKanjisJson("""["丁"]"""); Unit },
            { repository().exportLearnedKanjisJson(); Unit },
        )
        for (operation in operations) {
            try {
                operation()
                org.junit.Assert.fail("Pending restoration must block the operation")
            } catch (_: ProgressRestorationPendingException) { }
            assertFalse(dao.isProgressInitialized())
            assertTrue(dao.getLearnedKanjisOnce().isEmpty())
            assertEquals("""{"a":["日","一"]}""", savedBackup)
        }
        coVerify(exactly = 0) { backup.writeBackupJson(any()) }

        coEvery { backup.readBackupJson() } answers { savedBackup }
        repository().toggleLearnedKanji("丁")
        assertTrue(dao.isProgressInitialized())
        assertEquals(setOf("日", "一", "丁"), dao.getLearnedKanjisOnce().map { it.kanji }.toSet())
        assertEquals(setOf("日", "一", "丁"),
            JsonParser.parseString(savedBackup).asJsonObject["learnedKanji"].asJsonArray.map { it.asString }.toSet())
    }

    @Test fun `malformed automatic backup cannot be replaced by an edit or a manual import`() = runTest {
        seedCatalog()
        val malformed = """{"a":["日",null]}"""
        coEvery { backup.readBackupJson() } returns malformed
        repository().ensureAllKanjisLoaded {}
        for (importing in listOf(false, true)) {
            try {
                if (importing) repository().importLearnedKanjisJson("""["一"]""")
                else repository().toggleLearnedKanji("一")
                org.junit.Assert.fail("A malformed pending backup must be preserved")
            } catch (_: ProgressRestorationPendingException) { }
        }
        assertFalse(database.kanjiDao().isProgressInitialized())
        assertTrue(database.kanjiDao().getLearnedKanjisOnce().isEmpty())
        coVerify(exactly = 0) { backup.writeBackupJson(any()) }
    }

    @Test fun `partial catalog blocks progress transfers without consuming the pending backup`() = runTest {
        val dao = database.kanjiDao()
        dao.insertAll(listOf(entity("一")))
        coEvery { backup.readBackupJson() } returns """{"a":["一","丁"]}"""
        for (operation in listOf<suspend () -> Unit>(
            { repository().importLearnedKanjisJson("""["一","丁"]"""); Unit },
            { repository().exportLearnedKanjisJson(); Unit },
            { repository().toggleLearnedKanji("一") },
        )) {
            try {
                operation()
                org.junit.Assert.fail("The catalog must be complete before modifying or exporting progress")
            } catch (_: KanjiCatalogIncompleteException) { }
        }
        assertFalse(dao.isProgressInitialized())
        assertTrue(dao.getLearnedKanjisOnce().isEmpty())
        coVerify(exactly = 0) { backup.readBackupJson() }
        coVerify(exactly = 0) { backup.writeBackupJson(any()) }

        seedCatalog()
        repository().ensureAllKanjisLoaded {}
        assertEquals(setOf("一", "丁"), dao.getLearnedKanjisOnce().map { it.kanji }.toSet())
    }

    @Test fun `partial legacy catalog preserves existing learned progress and backup`() = runTest {
        val dao = database.kanjiDao()
        dao.insertAll(listOf(entity(learned = true)))
        dao.initializeProgress()
        try {
            repository().toggleLearnedKanji("日")
            org.junit.Assert.fail("A partial legacy catalog must not replace the automatic backup")
        } catch (_: KanjiCatalogIncompleteException) { }
        assertTrue(dao.isKanjiLearned("日"))
        coVerify(exactly = 0) { backup.writeBackupJson(any()) }
    }

    @Test fun `export restores pending old progress before producing the file contents`() = runTest {
        seedCatalog()
        coEvery { backup.readBackupJson() } returns """{"a":["日","一"]}"""
        val exported = JsonParser.parseString(repository().exportLearnedKanjisJson()).asJsonObject
        assertEquals(setOf("日", "一"), exported["learnedKanji"].asJsonArray.map { it.asString }.toSet())
        assertTrue(database.kanjiDao().isProgressInitialized())
    }

    @Test fun `automatic restoration preserves backups containing characters missing from the catalog`() = runTest {
        seedCatalog()
        coEvery { backup.readBackupJson() } returns """{"a":["日","月"]}"""
        repository().ensureAllKanjisLoaded {}
        assertFalse(database.kanjiDao().isProgressInitialized())
        assertTrue(database.kanjiDao().getLearnedKanjisOnce().isEmpty())
        coVerify(exactly = 0) { backup.writeBackupJson(any()) }

        database.kanjiDao().insertAll(listOf(entity("月")))
        repository().ensureAllKanjisLoaded {}
        assertEquals(setOf("日", "月"), database.kanjiDao().getLearnedKanjisOnce().map { it.kanji }.toSet())
    }
}
