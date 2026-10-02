package com.app.kanjistudy.presentation.scan.image

import android.app.Application
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.app.kanjistudy.testing.MainDispatcherRule
import com.google.android.gms.tasks.TaskCompletionSource
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class ImagePickerLaunchTest {
    @get:Rule val main = MainDispatcherRule()

    private class Screen : LifecycleOwner {
        override val lifecycle = LifecycleRegistry.createUnsafe(this).apply {
            currentState = Lifecycle.State.RESUMED
        }
    }

    @Test fun `scanner ready in background launches once after resume`() = runTest {
        val screen = Screen()
        val result = TaskCompletionSource<String>()
        val launches = mutableListOf<String>()
        var aborted = false
        launch {
            launchImagePicker(screen.lifecycle, { result.task.awaitScannerIntent() },
                { launches.add(it) }, { fail("Unexpected gallery fallback") }, { aborted = true })
        }
        runCurrent()
        screen.lifecycle.currentState = Lifecycle.State.CREATED
        result.setResult("scanner")
        runCurrent()
        assertTrue(launches.isEmpty())
        assertFalse(aborted)
        screen.lifecycle.currentState = Lifecycle.State.RESUMED
        runCurrent()
        assertEquals(listOf("scanner"), launches)
        screen.lifecycle.currentState = Lifecycle.State.CREATED
        screen.lifecycle.currentState = Lifecycle.State.RESUMED
        runCurrent()
        assertEquals(1, launches.size)
        assertFalse(aborted)
    }

    @Test fun `failed preparation in background waits before gallery fallback`() = runTest {
        val screen = Screen()
        val result = TaskCompletionSource<String>()
        var galleryLaunches = 0
        launch {
            launchImagePicker(screen.lifecycle, { result.task.awaitScannerIntent() },
                { fail("Unexpected scanner") }, { galleryLaunches++ }, { fail("Unexpected abort") })
        }
        runCurrent()
        screen.lifecycle.currentState = Lifecycle.State.CREATED
        result.setException(IOException("Scanner unavailable"))
        runCurrent()
        assertEquals(0, galleryLaunches)
        screen.lifecycle.currentState = Lifecycle.State.RESUMED
        runCurrent()
        assertEquals(1, galleryLaunches)
    }

    @Test fun `leaving screen cancels preparation and ignores late result`() = runTest {
        val screen = Screen()
        val result = TaskCompletionSource<String>()
        var aborted = false
        val job = launch {
            launchImagePicker(screen.lifecycle, { result.task.awaitScannerIntent() },
                { fail("Stale scanner launch") }, { fail("Stale gallery launch") }, { aborted = true })
        }
        runCurrent()
        job.cancel()
        runCurrent()
        assertTrue(aborted)
        assertTrue(job.isCompleted)
        result.setResult("late result")
        runCurrent()
    }

    @Test fun `destroying screen while waiting to resume releases selection`() = runTest {
        val screen = Screen()
        screen.lifecycle.currentState = Lifecycle.State.CREATED
        var aborted = false
        val job = launch {
            launchImagePicker(screen.lifecycle, { "scanner" },
                { fail("Stale scanner launch") }, { fail("Unexpected gallery") }, { aborted = true })
        }
        runCurrent()
        screen.lifecycle.currentState = Lifecycle.State.DESTROYED
        runCurrent()
        assertTrue(aborted)
        assertTrue(job.isCancelled)
    }

    @Test fun `launch failure releases selection for retry`() = runTest {
        val screen = Screen()
        var aborted = false
        launchImagePicker(screen.lifecycle, { "scanner" },
            { throw IllegalStateException("Launcher unavailable") }, {}, { aborted = true })
        assertTrue(aborted)
    }
}
