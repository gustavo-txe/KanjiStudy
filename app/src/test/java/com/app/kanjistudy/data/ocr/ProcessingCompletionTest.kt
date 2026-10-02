package com.app.kanjistudy.data.ocr

import android.app.Application
import com.google.android.gms.tasks.CancellationTokenSource
import com.google.android.gms.tasks.TaskCompletionSource
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class ProcessingCompletionTest {
    @Test fun `cancellation waits for native task before releasing image owner`() = runTest {
        val source = TaskCompletionSource<String>()
        var released = false
        var published = false
        val job = launch {
            source.task.awaitProcessingCompletion()
            published = true
        }
        job.invokeOnCompletion { released = true }
        runCurrent()
        job.cancel()
        runCurrent()
        assertFalse(released)
        source.setResult("日")
        runCurrent()
        assertTrue(released)
        assertTrue(job.isCancelled)
        assertFalse(published)
    }

    @Test fun `completed recognition returns text and failures propagate`() = runTest {
        val success = TaskCompletionSource<String>().apply { setResult("日") }
        assertEquals("日", success.task.awaitProcessingCompletion())
        val failure = IOException("OCR failed")
        val failed = TaskCompletionSource<String>().apply { setException(failure) }
        try {
            failed.task.awaitProcessingCompletion()
            fail("Expected recognition failure")
        } catch (actual: IOException) {
            // Coroutine stack-trace recovery can copy exceptions across suspension points.
            assertEquals(failure.message, actual.message)
        }
    }

    @Test fun `native task cancellation completes waiting coroutine`() = runTest {
        val cancellation = CancellationTokenSource()
        val source = TaskCompletionSource<String>(cancellation.token)
        val job = launch { source.task.awaitProcessingCompletion() }
        runCurrent()
        cancellation.cancel()
        // Token cancellation listeners are dispatched by Google Tasks on main.
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        runCurrent()
        assertTrue(job.isCompleted)
        assertTrue(job.isCancelled)
    }
}
