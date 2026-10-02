package com.app.kanjistudy.data.document

import android.app.Application
import android.net.Uri
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class JsonDocumentStoreTest {
    @get:Rule val temp = TemporaryFolder()

    @Test fun `writing truncates previous content and preserves Japanese UTF8 text`() = runTest {
        val file = temp.newFile("backup.json")
        file.writeText("a much longer existing document")
        val documents = JsonDocumentStore(RuntimeEnvironment.getApplication(), StandardTestDispatcher(testScheduler))
        val uri = Uri.fromFile(file)
        documents.write(uri, "[\"日\"]")
        assertEquals("[\"日\"]", file.readText(Charsets.UTF_8))
        assertEquals("[\"日\"]", documents.read(uri))
    }
    @Test fun `oversized backup is rejected before it can exhaust memory`() = runTest {
        val file = temp.newFile("large.json")
        file.writeText(" ".repeat(1_048_577))
        val documents = JsonDocumentStore(RuntimeEnvironment.getApplication(), StandardTestDispatcher(testScheduler))
        try {
            documents.read(Uri.fromFile(file))
            fail("Oversized backup must be rejected")
        } catch (_: IllegalArgumentException) {
        }
    }
}
