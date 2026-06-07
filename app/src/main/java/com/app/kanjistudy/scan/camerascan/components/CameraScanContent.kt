package com.app.kanjistudy.scan.camerascan.components

import android.content.Intent
import androidx.camera.core.ImageProxy
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.kanjistudy.R
import com.app.kanjistudy.onboarding.OnboardingHighlight
import com.app.kanjistudy.onboarding.OnboardingManager
import com.app.kanjistudy.onboarding.OnboardingOverlay
import com.app.kanjistudy.scan.camerascan.CameraScanUiState
import com.app.kanjistudy.scan.imagescan.ImageKanjiScanActivity
import kotlinx.coroutines.delay

private var hasShownScanInstructionsInProcess = false

@Composable
fun CameraScanContent(
    uiState: CameraScanUiState,
    onboardingManager: OnboardingManager,
    onKanjiClick: (Char) -> Unit,
    onPauseToggle: () -> Unit,
    onFrame: (ImageProxy) -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var showScanHint by remember { mutableStateOf(onboardingManager.shouldShowHint("scan")) }
    var showScanInstructions by remember { mutableStateOf(!hasShownScanInstructionsInProcess) }
    var instructionBounds by remember { mutableStateOf<Rect?>(null) }
    var pauseFabCenterX by remember { mutableStateOf(0.dp) }
    var pauseFabCenterY by remember { mutableStateOf(0.dp) }

    LaunchedEffect(showScanInstructions) {
        if (showScanInstructions) {
            hasShownScanInstructionsInProcess = true
            delay(2_000)
            showScanInstructions = false
        } else {
            instructionBounds = null
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        CameraPreview(onFrame = onFrame)
        CameraGradientScrim()
        DetectedKanjiLayer(
            uiState = uiState,
            showInstructions = showScanInstructions,
            onInstructionPositioned = { instructionBounds = it },
            onKanjiClick = onKanjiClick
        )

        if (showScanInstructions) {
            InstructionHintOverlay(rect = instructionBounds)
        }

        ScanImageButton(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(20.dp),
            onClick = {
                context.startActivity(Intent(context, ImageKanjiScanActivity::class.java))
            },
        )

        FloatingActionButton(
            onClick = onPauseToggle,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .onGloballyPositioned { coordinates ->
                    val fabCenterInParent = coordinates.boundsInParent().center
                    pauseFabCenterX = with(density) { fabCenterInParent.x.toDp() }
                    pauseFabCenterY = with(density) { fabCenterInParent.y.toDp() }
                },
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ) {
            Icon(
                painter = if (uiState.isPaused) {
                    painterResource(id = R.drawable.baseline_play_arrow_24)
                } else {
                    painterResource(id = R.drawable.baseline_pause_24)
                },
                contentDescription = if (uiState.isPaused) "Resume scan" else "Pause scan",
            )
        }

        if (showScanHint && pauseFabCenterX > 0.dp) {
            OnboardingOverlay(
                message = "Scan\n\nUse your camera to identify kanji. Tap a kanji to get details using Google AI." +
                        "\n\nTap the bottom-right button to pause the scan." +
                        "\n\nStylized kanji, decorative fonts, or image quality can make identification more difficult.",
                onDismiss = {
                    onboardingManager.markHintShown("scan")
                    showScanHint = false
                },
                highlight = OnboardingHighlight(
                    centerX = pauseFabCenterX,
                    centerY = pauseFabCenterY,
                    radius = 40.dp,
                ),
            )
        }
    }
}

@Composable
private fun CameraGradientScrim() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color.Black.copy(alpha = 0.36f),
                        Color.Transparent,
                        Color.Black.copy(alpha = 0.48f),
                    ),
                ),
            ),
    )
}

@Composable
private fun DetectedKanjiLayer(
    uiState: CameraScanUiState,
    showInstructions: Boolean,
    onInstructionPositioned: (Rect) -> Unit,
    onKanjiClick: (Char) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        AnimatedVisibility(
            visible = showInstructions,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Card(
                modifier = Modifier
                    .padding(top = 48.dp, start = 24.dp, end = 24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
                    contentColor = MaterialTheme.colorScheme.onSurface,
                ),
                shape = RoundedCornerShape(24.dp),
            ) {
                Text(
                    text = "Tap a kanji for details. Pause the camera to review results.",
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )
            }
        }

        LazyVerticalGrid(
            columns = GridCells.Fixed(4),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(top = 8.dp, bottom = 96.dp, start = 12.dp, end = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            items(uiState.kanji.toList()) { char ->
                DetectedKanjiCard(
                    kanji = char,
                    isLearned = uiState.learnedKanjis.contains(char),
                    onClick = { onKanjiClick(char) },
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun DetectedKanjiCard(
    kanji: Char,
    isLearned: Boolean,
    onClick: () -> Unit,
) {
    val containerColor = if (isLearned) {
        Color(0x4D4CAF50)
    } else {
        Color(0x80333333)
    }
    val contentColor = Color(0xFFFFFFFF)
    val cardShape = RoundedCornerShape(20.dp)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(cardShape)
            .background(containerColor)
            .clickable(
                onClick = onClick
            ),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = kanji.toString(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            color = contentColor,
            fontSize = 44.sp,
            lineHeight = 48.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Medium,
        )
    }
}



@Composable
private fun ScanImageButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    val gradient = rememberAnimatedImageScanGradient()

    FilledTonalButton(
        onClick = onClick,
        modifier = modifier
            .clip(RoundedCornerShape(22.dp))
            .background(gradient)
            .defaultMinSize(minHeight = 54.dp),
        colors = ButtonDefaults.filledTonalButtonColors(
            containerColor = Color.Transparent,
            contentColor = Color.White,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(id = R.drawable.baseline_camera),
                contentDescription = null,
                tint = Color.White,
            )
            Text(
                text = "Scan Image",
                modifier = Modifier.padding(start = 8.dp),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
            )
        }
    }
}

@Composable
private fun rememberAnimatedImageScanGradient(): Brush {
    val transition = rememberInfiniteTransition(label = "image_scan_gradient_transition")
    val progress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 20_000
                0f at 0 using LinearEasing
                1f at 10_000 using LinearEasing
                0f at 20_000 using LinearEasing
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "image_scan_shimmer_progress",
    )
    val gradientShift = -320f + (640f * progress)

    return Brush.linearGradient(
        colors = listOf(
            Color(0xFF102A6B),
            Color(0xFF003AE7),
            Color(0xFF135D96),
            Color(0xFF0F4891),
            Color(0xFF0730C4),
        ),
        start = Offset(gradientShift, 0f),
        end = Offset(gradientShift + 680f, 0f),
    )
}

@Composable
private fun InstructionHintOverlay(rect: Rect?) {
    if (rect == null) return

    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen),
    ) {
        drawRect(color = Color.Black.copy(alpha = 0.75f))

        val horizontalPadding = 20.dp.toPx()
        val verticalPadding = 2.dp.toPx()
        val cornerRadius = 24.dp.toPx()

        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(
                x = rect.left - horizontalPadding,
                y = rect.top - verticalPadding,
            ),
            size = Size(
                width = rect.width + (horizontalPadding * 2),
                height = rect.height + (verticalPadding * 2),
            ),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            blendMode = BlendMode.Clear,
        )
    }
}