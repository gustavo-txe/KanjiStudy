package com.app.kanjistudy.data.ocr

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import java.util.concurrent.CancellationException
import java.util.concurrent.Executor
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class MlKitJapaneseTextRecognizer @Inject constructor() : TextRecognizer {

    private val recognizer = TextRecognition.getClient(
        JapaneseTextRecognizerOptions.Builder().build()
    )

    override suspend fun recognize(image: InputImage): String =
        recognizer.process(image).awaitProcessingCompletion().text
}

// ML Kit keeps using the image after coroutine cancellation. Do not release its
// owner until the native task has actually completed, then propagate cancellation.
internal suspend fun <T> Task<T>.awaitProcessingCompletion(): T {
    val callerContext = currentCoroutineContext()
    val result = withContext(NonCancellable) {
        suspendCoroutine<T> { continuation ->
            addOnCompleteListener(Executor { it.run() }) { task ->
                when {
                    task.isCanceled -> continuation.resumeWithException(CancellationException("OCR cancelled"))
                    task.isSuccessful -> continuation.resume(task.result)
                    else -> continuation.resumeWithException(checkNotNull(task.exception))
                }
            }
        }
    }
    callerContext.ensureActive()
    return result
}
