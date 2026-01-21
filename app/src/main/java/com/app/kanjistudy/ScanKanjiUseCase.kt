package com.app.kanjistudy

import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

class ScanKanjiUseCase @Inject constructor(
    private val recognizer: TextRecognizer
) {

    private var lastUpdateTime = 0L
    private val throttleMs = 2500L

    suspend fun analyze(
        image: InputImage,
        isPaused: Boolean,
        lastRecognizedKanji: String?
    ): String? {

        if (isPaused) return null

        val now = System.currentTimeMillis()
        if (now - lastUpdateTime < throttleMs) return null

        val text = recognizer.recognize(image)

        val kanjisOnly = text.filter {
            val code = it.code
            code in 0x4E00..0x9FFF
        }

        if (kanjisOnly.isBlank() || kanjisOnly == lastRecognizedKanji) {
            return null
        }

        lastUpdateTime = now
        return kanjisOnly
    }

}
