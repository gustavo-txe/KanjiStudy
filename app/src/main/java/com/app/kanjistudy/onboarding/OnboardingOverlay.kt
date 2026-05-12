package com.app.kanjistudy.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun OnboardingOverlay(
    message: String,
    onDismiss: () -> Unit,
    highlight: OnboardingHighlight? = null,
    textAlignment: Alignment = Alignment.Center,
    textBottomPadding: Dp = 56.dp
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
            .clickable(onClick = onDismiss)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawRect(color = Color.Black.copy(alpha = 0.75f))

            highlight?.let {
                val left = it.centerX.toPx() - it.radius.toPx()
                val top = it.centerY.toPx() - it.radius.toPx()
                drawOval(
                    color = Color.Transparent,
                    topLeft = Offset(left, top),
                    size = Size(it.radius.toPx() * 2, it.radius.toPx() * 2),
                    blendMode = BlendMode.Clear
                )
            }
        }

        Text(
            text = message,
            color = Color.White,
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
                .align(textAlignment)
                .padding(horizontal = 24.dp)
                .background(Color.Black.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        )

        TextButton(
            onClick = onDismiss,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = textBottomPadding)
        ) {
            Text("Got it", color = Color.White)
        }
    }
}

data class OnboardingHighlight(
    val centerX: Dp,
    val centerY: Dp,
    val radius: Dp
)