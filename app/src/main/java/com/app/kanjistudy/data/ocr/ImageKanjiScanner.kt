package com.app.kanjistudy.data.ocr

import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

class ImageKanjiScanner @Inject constructor(
    private val recognizer: TextRecognizer,
) {
    suspend operator fun invoke(image: InputImage): String {
        val text = recognizer.recognize(image)
        return text.filter {
            val code = it.code
            code in 0x4E00..0x9FFF
        }
    }
}
