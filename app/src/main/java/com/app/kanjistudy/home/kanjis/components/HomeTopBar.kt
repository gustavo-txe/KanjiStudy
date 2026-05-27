package com.app.kanjistudy.home.kanjis.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.kanjistudy.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar(
    isDarkTheme: Boolean,
    showJlptFilter: Boolean,
    selectedJlptLevel: Int?,
    onOpenDrawer: () -> Unit,
    onOpenJlptFilter: () -> Unit,
    onThemeChanged: (Boolean) -> Unit,
    showBackButton: Boolean = false,
    onBackClick: (() -> Unit)? = null
) {

    TopAppBar(
        navigationIcon = {
            if (showBackButton && onBackClick != null) {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                }
            } else {
                IconButton(onClick = onOpenDrawer) {
                    Icon(Icons.Default.Menu, contentDescription = "Open menu")
                }
            }
        },
        title = { AnimatedTitle(isDarkTheme = isDarkTheme) },
        actions = {
            Row {
                if (showJlptFilter) {
                    AssistChip(
                        onClick = onOpenJlptFilter,
                        label = { Text(selectedJlptLevel?.let { "N$it" } ?: "ALL") },
                        leadingIcon = { Icon(Icons.Default.FilterList, contentDescription = null) },
                        colors = AssistChipDefaults.assistChipColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        )
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    )
}

@Composable
fun AnimatedTitle(isDarkTheme: Boolean) {
    val infiniteTransition = rememberInfiniteTransition(label = "infiniteColor")
    val shineWidth = 250f
    val animatedColorTitle = infiniteTransition.animateFloat(
        initialValue = -shineWidth,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "colorCycle"
    ).value

    val brush = Brush.linearGradient(
        colors = listOf(Color.Transparent, Color.White.copy(0.75f), Color.Transparent),
        start = Offset(animatedColorTitle, 0f),
        end = Offset(animatedColorTitle + shineWidth, 0f)
    )

    val kouzanBrush = FontFamily(Font(R.font.kouzanbrush))
    Box(
        modifier = Modifier
            .drawWithContent {
                drawContent()
                if (!isDarkTheme) {
                    drawRect(brush = brush, blendMode = BlendMode.Lighten)
                }
            }
            .padding(top = 8.dp)
    ) {
        Text(
            text = "常用漢字",
            fontSize = 30.sp,
            fontFamily = kouzanBrush,
            fontWeight = FontWeight.Bold,
            color = if (isDarkTheme) Color.White else Color.Black
        )
    }
}