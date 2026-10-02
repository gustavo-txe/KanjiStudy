package com.app.kanjistudy.data.ocr

import com.app.kanjistudy.core.time.MonotonicClock
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

class CameraKanjiScanner @Inject constructor(
    private val recognizer: TextRecognizer,
    private val clock: MonotonicClock
) {

    private var lastUpdateTime: Long? = null
    private val throttleMs = 2500L

    suspend fun analyze(
        image: InputImage,
        lastRecognizedKanji: String?
    ): String? {

        val now = clock.nowMillis()
        if (lastUpdateTime?.let { now - it < throttleMs } == true) return null
        lastUpdateTime = now

        val text = recognizer.recognize(image)

        val kanjisOnly = text.filter {
            val code = it.code
            code in 0x4E00..0x9FFF
        }.toSet().joinToString("")

        if (kanjisOnly.isBlank() || kanjisOnly == lastRecognizedKanji) {
            return null
        }

        return kanjisOnly
    }

}
