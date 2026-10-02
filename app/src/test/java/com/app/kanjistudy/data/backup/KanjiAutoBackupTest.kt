package com.app.kanjistudy.data.backup

import android.app.Application
import java.io.File
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class KanjiAutoBackupTest {
    @Test fun `missing or empty backup returns null`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val file = File(context.filesDir, "learned_kanji_auto_backup.json")
        file.delete()
        val backup = KanjiAutoBackup(context, StandardTestDispatcher(testScheduler))
        assertNull(backup.readBackupJson())
        file.writeText("")
        assertNull(backup.readBackupJson())
    }

    @Test fun `UTF8 backup survives a new instance and replacement leaves no temporary file`() = runTest {
        val context = RuntimeEnvironment.getApplication()
        val dispatcher = StandardTestDispatcher(testScheduler)
        val backup = KanjiAutoBackup(context, dispatcher)
        backup.writeBackupJson("[\"日\",\"月\"]")
        assertEquals("[\"日\",\"月\"]", KanjiAutoBackup(context, dispatcher).readBackupJson())
        backup.writeBackupJson("[\"火\"]")
        backup.writeBackupJson("[\"火\"]")
        assertEquals("[\"火\"]", backup.readBackupJson())
        assertFalse(File(context.filesDir, "learned_kanji_auto_backup.json.tmp").exists())
    }
}
