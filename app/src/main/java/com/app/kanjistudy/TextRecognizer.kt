package com.app.kanjistudy

import com.google.mlkit.vision.common.InputImage

interface TextRecognizer {
    suspend fun recognize(image: InputImage): String

}