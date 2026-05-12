package com.app.kanjistudy.scan.usecase

import com.app.kanjistudy.scan.recognizer.TextRecognizer
import com.google.mlkit.vision.common.InputImage
import javax.inject.Inject

class ImageScanFromGallery @Inject constructor(
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