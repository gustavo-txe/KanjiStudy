package com.app.kanjistudy.data.local

import android.app.Application
import androidx.room.Room
import com.app.kanjistudy.testing.entity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class KanjiDaoTest {
    private lateinit var database: AppDatabase
    private lateinit var dao: KanjiDao

    @Before fun setUp() {
        database = Room.inMemoryDatabaseBuilder(RuntimeEnvironment.getApplication(), AppDatabase::class.java).build()
        dao = database.kanjiDao()
    }

    @After fun tearDown() = database.close()

    @Test fun `insert preserves existing progress and round trips Japanese readings`() = runTest {
        val original = entity(learned = true).copy(meanings = listOf("sun, daylight", "quoted \"word\"", "line\nbreak"))
        dao.insertAll(listOf(original))
        dao.insertAll(listOf(entity().copy(meanings = listOf("replacement"))))
        assertEquals(listOf(original), dao.getAllKanjis())
        assertEquals(1, dao.countKanjis())
    }

    @Test fun `toggle updates learned queries and unknown character affects no rows`() = runTest {
        dao.insertAll(listOf(entity(), entity("月")))
        assertTrue(dao.getLearnedKanjis().first().isEmpty())
        assertEquals(1, dao.toggleLearned("日"))
        assertEquals(listOf(entity(learned = true)), dao.getLearnedKanjis().first())
        assertTrue(dao.isKanjiLearned("日"))
        assertEquals(1, dao.toggleLearned("日"))
        assertTrue(dao.getLearnedKanjisOnce().isEmpty())
        assertEquals(0, dao.toggleLearned("外"))
    }

    @Test fun `import merges progress and supports more than one SQLite batch`() = runTest {
        val entries = (0x4E00 until 0x4E00 + 1001).map { entity(it.toChar().toString()) }
        dao.insertAll(entries + entity("月", true))
        assertEquals(1001, dao.importLearnedKanjis(entries.map { it.kanji } + "unknown"))
        assertEquals(1002, dao.getLearnedKanjisOnce().size)
        assertTrue(dao.isKanjiLearned("月"))
        assertEquals(0, dao.importLearnedKanjis(emptyList()))
    }

    @Test fun `lookup excludes unknown characters and refresh does not import learned flags`() = runTest {
        dao.insertAll(listOf(entity(), entity("月")))
        val updated = entity(learned = true).copy(jlpt = null, meanings = emptyList())
        dao.refreshMetadata(listOf(updated))
        assertEquals(listOf(updated.copy(isLearned = false)), dao.getKanjisByChars(listOf("日", "外")))
        assertEquals(listOf("日"), dao.getExistingKanjiChars(listOf("日", "外")))
        assertEquals(1, dao.countKanjisMissingJlpt())
    }

    @Test fun `metadata refresh preserves marks and removals made after download started`() = runTest {
        dao.insertAll(listOf(entity(), entity("月", true)))
        val downloaded = dao.getAllKanjis().map { it.copy(meanings = listOf("updated")) }
        dao.toggleLearned("日")
        dao.toggleLearned("月")
        dao.refreshMetadata(downloaded + entity("明", true))
        assertTrue(dao.isKanjiLearned("日"))
        assertFalse(dao.isKanjiLearned("月"))
        assertFalse(dao.isKanjiLearned("明"))
        assertEquals(listOf("updated"), dao.getKanjisByChars(listOf("日")).single().meanings)
    }

    @Test fun `failed second import batch rolls back the entire import and preserves previous progress`() = runTest {
        val entries = (0x4E00 until 0x4E00 + 1001).map { entity(it.toChar().toString()) }
        val previous = entity("月", true)
        dao.insertAll(entries + previous)
        val failingKanji = entries[500].kanji
        database.openHelper.writableDatabase.execSQL(
            "CREATE TRIGGER reject_import BEFORE UPDATE OF isLearned ON kanji_table " +
                "WHEN NEW.kanji = '$failingKanji' BEGIN SELECT RAISE(ABORT, 'import blocked'); END"
        )
        try {
            dao.importLearnedKanjis(entries.map { it.kanji })
            fail("An import interrupted during the second batch must fail")
        } catch (exception: android.database.sqlite.SQLiteException) {
            assertTrue(exception.message.orEmpty().contains("import blocked"))
        }
        assertEquals(listOf(previous), dao.getLearnedKanjisOnce())
        assertFalse(dao.isProgressInitialized())
    }
}
