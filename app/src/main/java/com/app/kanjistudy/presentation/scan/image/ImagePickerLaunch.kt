package com.app.kanjistudy.presentation.scan.image

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.withResumed
import com.google.android.gms.tasks.Task
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine

internal suspend fun <T : Any> launchImagePicker(
    lifecycle: Lifecycle,
    prepareScanner: suspend () -> T?,
    launchScanner: (T) -> Unit,
    launchGallery: () -> Unit,
    onLaunchAborted: () -> Unit,
) {
    var launched = false
    try {
        val scannerIntent = try {
            prepareScanner()
        } catch (exception: CancellationException) {
            throw exception
        } catch (_: Exception) {
            // Unsupported devices and unavailable Play services use the gallery.
            null
        }
        lifecycle.withResumed {
            if (scannerIntent == null) launchGallery() else launchScanner(scannerIntent)
            launched = true
        }
    } catch (exception: CancellationException) {
        throw exception
    } catch (_: Exception) {
        // A failed launch must leave the selection button available for another try.
    } finally {
        // Once launched, only the Activity Result callback should finish selection.
        if (!launched) onLaunchAborted()
    }
}

internal suspend fun <T> Task<T>.awaitScannerIntent(): T = suspendCancellableCoroutine { continuation ->
    // Do not bind to Activity: those listeners are removed on stop, losing the result.
    addOnCompleteListener(Executor { it.run() }) { task ->
        when {
            task.isCanceled -> continuation.cancel()
            task.isSuccessful -> continuation.resume(task.result)
            else -> continuation.resumeWithException(checkNotNull(task.exception))
        }
    }
}
