package com.app.kanjistudy.scan.mlkit

import com.app.kanjistudy.scan.recognizer.TextRecognizer
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class MlKitJapaneseTextRecognizer @Inject constructor() : TextRecognizer {

    private val recognizer = TextRecognition.getClient(
        JapaneseTextRecognizerOptions.Builder().build()
    )

    override suspend fun recognize(image: InputImage): String =
        suspendCancellableCoroutine { cont ->

            recognizer.process(image)
                .addOnSuccessListener { result ->
                    if (cont.isActive) {
                        cont.resume(result.text)
                    }
                }
                .addOnFailureListener { e ->
                    if (cont.isActive) {
                        cont.resumeWithException(e)
                    }
                }
        }
}