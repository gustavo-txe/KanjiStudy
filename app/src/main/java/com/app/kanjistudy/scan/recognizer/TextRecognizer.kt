package com.app.kanjistudy.scan.recognizer

import com.google.mlkit.vision.common.InputImage

interface TextRecognizer {
    suspend fun recognize(image: InputImage): String

}