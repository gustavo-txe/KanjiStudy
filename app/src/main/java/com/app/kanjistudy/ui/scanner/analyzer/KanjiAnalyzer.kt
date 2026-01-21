package com.app.kanjistudy.ui.scanner.analyzer

import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy

class KanjiAnalyzer(
    private val onFrame: (ImageProxy) -> Unit
) : ImageAnalysis.Analyzer {

    override fun analyze(imageProxy: ImageProxy) {
        onFrame(imageProxy)
    }
}
