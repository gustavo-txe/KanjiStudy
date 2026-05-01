package com.app.kanjistudy.home.kanjis.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.kanjistudy.home.kanjis.KanjiViewModel

@Composable
fun KanjiLoadingScreen(
    viewModel: KanjiViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val progress = viewModel.uiState.collectAsStateWithLifecycle().value.loadingProgress
    val isLoading = viewModel.uiState.collectAsStateWithLifecycle().value.isLoading
    val infiniteTransition = rememberInfiniteTransition(label = "infiniteColor1")
    val shineWidth = 500f

    val animatedColorTitle by infiniteTransition.animateFloat(
        initialValue = -shineWidth,
        targetValue = 500f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "colorCycle1"
    )

    val brush = Brush.linearGradient(
        colors = if (isSystemInDarkTheme()) {
            listOf(
                Color.Transparent,
                Color.Black,
                Color.Transparent
            )
        } else {
            listOf(
                Color.Transparent,
                Color.White.copy(0.6f),
                Color.Transparent
            )
        },
        start = Offset(animatedColorTitle, 0f),
        end = Offset(animatedColorTitle + shineWidth, 0f)
    )

    if (isLoading) {
        Column(
            modifier = modifier
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = ProgressIndicatorDefaults.linearColor,
                trackColor = ProgressIndicatorDefaults.linearTrackColor,
                strokeCap = ProgressIndicatorDefaults.LinearStrokeCap,
            )
            Spacer(modifier = Modifier.height(16.dp))

            Box(
                modifier = Modifier
                    .drawWithContent {
                        drawContent()
                        drawRect(brush = brush, blendMode = BlendMode.Lighten)
                    }
                    .padding(top = 8.dp)
                    .align(Alignment.CenterHorizontally)
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "Downloading kanji. Please wait...",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,

                        )
                    Text(
                        text = "${(progress * 100).toInt()}%",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,

                        )
                }
            }
        }
    }
}