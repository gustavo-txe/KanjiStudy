package com.app.kanjistudy.scan.camerascan.components

import android.util.Log
import android.util.Size as AndroidSize
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.app.kanjistudy.scan.analyzer.KanjiAnalyzer
import java.util.concurrent.Executor
import java.util.concurrent.Executors

private const val CAMERA_LOG_TAG = "CameraPreview"

@Composable
fun CameraPreview(
    onFrame: (ImageProxy) -> Unit,
) {
    val context = LocalContext.current
    val applicationContext = context.applicationContext
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnFrame by rememberUpdatedState(onFrame)
    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
        }
    }

    DisposableEffect(Unit) {
        onDispose { analysisExecutor.shutdown() }
    }

    DisposableEffect(applicationContext, lifecycleOwner, previewView) {
        var isDisposed = false
        val cameraProviderFuture = ProcessCameraProvider.getInstance(applicationContext)

        cameraProviderFuture.addListener(
            {
                if (!isDisposed) {
                    val cameraProvider = runCatching { cameraProviderFuture.get() }
                        .getOrElse { exception ->
                            Log.e(
                                CAMERA_LOG_TAG,
                                "Unable to obtain the camera provider.",
                                exception
                            )
                            null
                        }

                    if (cameraProvider != null) {
                        val preview = Preview.Builder().build().also { preview ->
                            preview.surfaceProvider = previewView.surfaceProvider
                        }
                        val imageAnalysis = buildImageAnalysis(
                            onFrame = { imageProxy -> currentOnFrame(imageProxy) },
                            analysisExecutor = analysisExecutor,
                        )

                        try {
                            cameraProvider.unbindAll()
                            cameraProvider.bindToLifecycle(
                                lifecycleOwner,
                                CameraSelector.DEFAULT_BACK_CAMERA,
                                preview,
                                imageAnalysis,
                            )
                        } catch (exception: Exception) {
                            Log.e(CAMERA_LOG_TAG, "Unable to bind camera use cases.", exception)
                        }
                    }
                }
            },
            ContextCompat.getMainExecutor(applicationContext),
        )

        onDispose {
            isDisposed = true
            if (cameraProviderFuture.isDone) {
                runCatching { cameraProviderFuture.get().unbindAll() }
            }
        }
    }

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { previewView },
    )
}

private fun buildImageAnalysis(
    onFrame: (ImageProxy) -> Unit,
    analysisExecutor: Executor,
): ImageAnalysis {
    return ImageAnalysis.Builder()
        .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
        .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
        .setOutputImageRotationEnabled(false)
        .setResolutionSelector(
            ResolutionSelector.Builder()
                .setResolutionStrategy(
                    ResolutionStrategy(
                        AndroidSize(1280, 720),
                        ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                    ),
                )
                .build(),
        )
        .build()
        .also { analysis ->
            analysis.setAnalyzer(
                analysisExecutor,
                KanjiAnalyzer(onFrame),
            )
        }
}