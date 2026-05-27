package com.app.kanjistudy.scan.camerascan

import android.Manifest
import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.kanjistudy.usecase.CustomTab
import com.app.kanjistudy.R
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Surface
import androidx.compose.material3.surfaceColorAtElevation
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.boundsInRoot
import com.app.kanjistudy.data.model.KanjiData
import com.app.kanjistudy.onboarding.OnboardingHighlight
import com.app.kanjistudy.onboarding.OnboardingManager
import com.app.kanjistudy.onboarding.OnboardingOverlay
import com.app.kanjistudy.scan.imagescan.ImageKanjiScanActivity
import com.app.kanjistudy.scan.analyzer.KanjiAnalyzer
import kotlinx.coroutines.delay
import java.util.concurrent.Executors
import kotlin.jvm.java
import android.util.Size as AndroidSize

private var showScanInstructionsProcess = false

@SuppressLint("ContextCastToActivity")
@Composable
fun ScanScreen(
    viewModel: CameraScanViewModel = hiltViewModel(),
    onboardingManager: OnboardingManager,
) {
    CameraPermission {
        CameraScreen(viewModel, onboardingManager)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CameraScreen(
    viewModel: CameraScanViewModel = hiltViewModel(),
    onboardingManager: OnboardingManager
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val customTab = remember { CustomTab() }

    val clipboardManager = LocalClipboardManager.current

    val context = LocalContext.current
    val density = LocalDensity.current

    var optionsKanji by remember { mutableStateOf<Char?>(null) }
    var dialogKanji by remember { mutableStateOf<Char?>(null) }
    var detailsKanji by remember { mutableStateOf<KanjiData?>(null) }
    var toggleLearnedKanji by remember { mutableStateOf<Char?>(null) }

    var showScanHint by remember { mutableStateOf(onboardingManager.shouldShowHint("scan")) }
    var showScanInstructions by remember {
        mutableStateOf(!showScanInstructionsProcess)
    }
    var pauseFabCenterX by remember { mutableStateOf(0.dp) }
    var pauseFabCenterY by remember { mutableStateOf(0.dp) }

    var rect by remember { mutableStateOf<Rect?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ScanUiEvent.CopyKanji -> {
                    clipboardManager.setText(
                        AnnotatedString(event.kanji.toString())
                    )
                    Toast.makeText(
                        context,
                        "Kanji copied!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
                ScanUiEvent.KanjiDownloadNotCompleted -> {
                    Toast.makeText(
                        context,
                        "Please wait until the kanji download is complete before marking a kanji as learned.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    LaunchedEffect(showScanInstructions) {
        if (showScanInstructions) {
            showScanInstructionsProcess = true
            delay(2000)
            showScanInstructions = false
        }
    }

    LaunchedEffect(showScanInstructions) {
        if (!showScanInstructions) {
            rect = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(viewModel = viewModel)
        Column(
            modifier = Modifier
                .fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            AnimatedVisibility(
                visible = showScanInstructions,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = "Click on a kanji for more details\nPause the camera for review.\n",
                        modifier = Modifier
                            .onGloballyPositioned { coordinates ->
                                rect = coordinates.boundsInParent()
                            },
                        color = Color.White,
                        fontSize = 16.sp,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        textAlign = TextAlign.Center
                    )
                }

            }
            LazyVerticalGrid(
                columns = GridCells.Fixed(4),
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(top = 8.dp, bottom = 90.dp),
            ) {
                items(uiState.kanji.toList()) { char ->
                    Text(
                        text = char.toString(),
                        modifier = Modifier
                            .padding(2.dp)
                            .combinedClickable(
                                onClick = {
                                    optionsKanji = char
                                },
                                onLongClick = {
                                    viewModel.onKanjiLongClick(char)
                                }
                            )
                            .fillMaxWidth(),
                        fontSize = 50.sp,
                        color = if (uiState.learnedKanjis.contains(char)) Color(0xFF4CAF50) else Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        if (showScanInstructions) {
            InstructionHintOverlay(
                rect = rect,
            )
        }

        val imageScanGradientTransition =
            rememberInfiniteTransition(label = "image_scan_gradient_transition")
        val shimmerProgress by imageScanGradientTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = keyframes {
                    durationMillis = 20000
                    0f at 0 using LinearEasing
                    1f at 10000 using LinearEasing
                    0f at 20000 using LinearEasing
                },
                repeatMode = RepeatMode.Restart
            ),
            label = "image_scan_shimmer_progress"
        )
        val gradientShift = -320f + (640f * shimmerProgress)

        val imageScanGradient = Brush.linearGradient(
            colors = listOf(
                Color(0xFF102A6B),
                Color(0xFF003AE7),
                Color(0xFF135D96),
                Color(0xFF0F4891),
                Color(0xFF0730C4)
            ),
            start = Offset(gradientShift, 0f),
            end = Offset(gradientShift + 680f, 0f)
        )
        FilledTonalButton(
            onClick = {
                context.startActivity(
                    Intent(
                        context,
                        ImageKanjiScanActivity::class.java
                    )
                )
            },
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp)
                .clip(RoundedCornerShape(20.dp))
                .background(imageScanGradient)
                .defaultMinSize(minHeight = 52.dp),
            colors = ButtonDefaults.filledTonalButtonColors(
                containerColor = Color.Transparent,
                contentColor = Color.White
            )
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    painter = painterResource(id = R.drawable.baseline_camera),
                    contentDescription = null,
                    tint = Color.White
                )
                Text(
                    text = "Scan Image",
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp,
                    color = Color.White
                )
            }
        }

        FloatingActionButton(
            onClick = { viewModel.togglePause() },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .onGloballyPositioned { coordinates ->
                    val fabCenterInParent = coordinates.boundsInParent().center
                    pauseFabCenterX = with(density) { fabCenterInParent.x.toDp() }
                    pauseFabCenterY = with(density) { fabCenterInParent.y.toDp() }
                }
        ) {

            Icon(
                painter = if (uiState.isPaused) painterResource(id = R.drawable.baseline_play_arrow_24)
                else painterResource(id = R.drawable.baseline_pause_24),
                contentDescription = if (uiState.isPaused) "Resume" else "Pause"
            )

        }

        if (showScanHint && pauseFabCenterX > 0.dp) {
            OnboardingOverlay(
                message = "Scan\n\nUse your camera to identify kanji. Tap a kanji for Google details" +
                        "\n\nClick the button in the bottom-right corner to pause the scan." +
                        "\n\nStylized kanji, decorative fonts\n" +
                        "or image quality can make \n" +
                        "identification more difficult.",
                onDismiss = {
                    onboardingManager.markHintShown("scan")
                    showScanHint = false
                },
                highlight = OnboardingHighlight(
                    centerX = pauseFabCenterX,
                    centerY = pauseFabCenterY,
                    radius = 40.dp
                ),
            )
        }
    }

    optionsKanji?.let { kanji ->
        val joyoKanji = uiState.recognizedJoyoKanjis[kanji]
        val isLearned = uiState.learnedKanjis.contains(kanji)
        AlertDialog(
            onDismissRequest = { optionsKanji = null },
            title = {
                Column(modifier = Modifier.fillMaxWidth()) {
                    if (joyoKanji != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            joyoKanji.jlpt?.let { jlpt ->
                                Text(
                                    text = "JLPT $jlpt",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            StatusToggleIcon(
                                isLearned = isLearned,
                                onToggle = { toggleLearnedKanji = kanji }
                            )
                        }
                    }
                    Text(
                        text = "$kanji",
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        fontSize = 100.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    dialogKanji = kanji
                    optionsKanji = null
                }) { Text("Search on Google") }
            },
            dismissButton = {
                Row {
                    TextButton(onClick = {
                        viewModel.onKanjiLongClick(kanji)
                        optionsKanji = null
                    }) { Text("Copy") }
                    if (joyoKanji != null) {
                        TextButton(onClick = {
                            detailsKanji = joyoKanji
                            optionsKanji = null
                        }) { Text("Details") }
                    }
                }
            }
        )
    }

    dialogKanji?.let { kanji ->
        AlertDialog(
            onDismissRequest = { dialogKanji = null },
            title = { Text("Open Google?") },
            text = {
                Text("Would you like to search for $kanji on Google?")
            },
            confirmButton = {
                TextButton(onClick = {
                    customTab.openCustomTab(
                        context,
                        "https://www.google.com/search?q=kanji+$kanji"
                    )
                    dialogKanji = null
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    dialogKanji = null
                }) {
                    Text("No")
                }
            }
        )
    }

    detailsKanji?.let { kanji ->
        val isLearned = uiState.learnedKanjis.contains(kanji.kanji.first())
        AlertDialog(
            onDismissRequest = { detailsKanji = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    kanji.jlpt?.let { jlpt ->
                        Text(
                            text = "JLPT $jlpt",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    StatusToggleIcon(
                        isLearned = isLearned,
                        onToggle = { toggleLearnedKanji = kanji.kanji.first() })
                }
            },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = kanji.kanji, fontSize = 96.sp, lineHeight = 98.sp)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceAround
                    ) {
                        DetailColumn(title = "Kun'yomi:", items = kanji.kunReadings)
                        DetailColumn(title = "On'yomi:", items = kanji.onReadings)
                        DetailColumn(title = "Meanings:", items = kanji.meanings)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { detailsKanji = null }) {
                    Text("Close")
                }
            }
        )
    }
    toggleLearnedKanji?.let { kanji ->
        val isLearned = uiState.learnedKanjis.contains(kanji)
        AlertDialog(
            onDismissRequest = { toggleLearnedKanji = null },
            title = {
                Text(if (isLearned) "Remove Kanji?" else "Add Kanji?")
            },
            text = {
                Text(
                    if (isLearned) "Would you like to remove the kanji $kanji as learned?"
                    else "Would you like to add the kanji $kanji as learned?"
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleLearnedKanji(kanji)
                    toggleLearnedKanji = null
                }) { Text("Yes") }
            },
            dismissButton = {
                TextButton(onClick = { toggleLearnedKanji = null }) { Text("No") }
            }
        )
    }
}

@Composable
private fun DetailColumn(title: String, items: List<String>) {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        items.forEach { Text(text = it, fontSize = 13.sp) }
    }
}

@Composable
fun CameraPreview(
    viewModel: CameraScanViewModel
) {
    val lifecycleOwner = LocalLifecycleOwner.current

    val analysisExecutor = remember { Executors.newSingleThreadExecutor() }

    DisposableEffect(Unit) {
        onDispose {
            analysisExecutor.shutdown()
        }
    }


    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            val previewView = PreviewView(ctx).apply {
                scaleType = PreviewView.ScaleType.FILL_CENTER
            }

            val cameraProviderFuture = ProcessCameraProvider.getInstance(ctx)

            cameraProviderFuture.addListener({

                val cameraProvider = cameraProviderFuture.get()

                val preview = Preview.Builder().build().also {
                    it.surfaceProvider = previewView.surfaceProvider
                }

                val imageAnalysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_YUV_420_888)
                    .setOutputImageRotationEnabled(false)
                    .setResolutionSelector(
                        ResolutionSelector.Builder()
                            .setResolutionStrategy(
                                ResolutionStrategy(
                                    AndroidSize(1280, 720),
                                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER
                                )
                            )
                            .build()
                    )
                    .build()
                    .also { analysis ->
                        analysis.setAnalyzer(
                            analysisExecutor,
                            KanjiAnalyzer { image ->
                                viewModel.onFrame(image)
                            }
                        )
                    }

                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        lifecycleOwner,
                        cameraSelector,
                        preview,
                        imageAnalysis
                    )
                } catch (exc: Exception) {
                    Log.e("CameraX", "Use case binding failed", exc)
                }

            }, ContextCompat.getMainExecutor(ctx))

            previewView
        }
    )
}

@Composable
private fun InstructionHintOverlay(
    rect: Rect?
) {
    if (rect == null) return

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
    ) {
        drawRect(color = Color.Black.copy(alpha = 0.75f))

        val horizontalPadding = 20.dp.toPx()
        val verticalPadding = 0.dp.toPx()
        val topPadding = 35.dp.toPx()
        val cornerRadius = 14.dp.toPx()

        val rectWidth = rect.width + (horizontalPadding * 2)
        val rectHeight = rect.height + (verticalPadding * 2)

        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(
                x = rect.left - horizontalPadding,
                y = rect.top - verticalPadding + topPadding
            ),
            size = Size(
                width = rectWidth,
                height = rectHeight
            ),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            blendMode = BlendMode.Clear
        )
    }
}

@Composable
fun CameraPermission(
    onPermissionGranted: @Composable () -> Unit
) {
    val context = LocalContext.current

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.CAMERA
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted: Boolean ->
        permissionGranted = isGranted
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (permissionGranted) {
        onPermissionGranted()
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text("Waiting for permission...")
        }
    }
}
