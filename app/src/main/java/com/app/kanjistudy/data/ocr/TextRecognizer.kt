package com.app.kanjistudy.data.ocr

import com.google.mlkit.vision.common.InputImage

interface TextRecognizer {
    /** Returns or throws only after the engine stops using [image], including on cancellation. */
    suspend fun recognize(image: InputImage): String

}
