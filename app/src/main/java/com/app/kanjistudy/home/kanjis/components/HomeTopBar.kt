package com.app.kanjistudy.home.kanjis.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.kanjistudy.usecase.CustomTab
import com.app.kanjistudy.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopBar() {
    val customTab = remember { CustomTab() }
    val context = LocalContext.current
    var expanded by remember { mutableStateOf(false) }
    val infiniteTransition = rememberInfiniteTransition(label = "infiniteColor")

    val shineWidth = 250f

    val animatedColorTitle by infiniteTransition.animateFloat(
        initialValue = -shineWidth,
        targetValue = 600f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 22000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "colorCycle"
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
                Color.White.copy(0.75f),
                Color.Transparent
            )
        },
        start = Offset(animatedColorTitle, 0f),
        end = Offset(animatedColorTitle + shineWidth, 0f)
    )

    TopAppBar(
        title = {
            AnimatedTitle(brush = brush)
        },
        actions = {
            IconButton(onClick = { expanded = true }) {
                Icon(
                    imageVector = Icons.Default.MoreVert,
                    contentDescription = "Menu"
                )
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text("Privacy Policy") },
                    onClick = {
                        expanded = false
                        customTab.openCustomTab(
                            context,
                            "https://sites.google.com/view/kanji-scanner-privacy-policy/home"
                        )
                    }
                )

                DropdownMenuItem(
                    text = { Text("Terms of Service") },
                    onClick = {
                        expanded = false
                        customTab.openCustomTab(
                            context,
                            "https://sites.google.com/view/kanji-scanner-terms-of-use/home"
                        )
                    }
                )
            }
        }
    )
}

@Composable
fun AnimatedTitle(brush: Brush) {
    val kouzanBrush = remember { FontFamily(Font(R.font.kouzanbrush)) }

    Box(
        modifier = Modifier
            .drawWithContent {
                drawContent()
                drawRect(brush = brush, blendMode = BlendMode.Lighten)
            }
            .padding(top = 8.dp)
    ) {
        Text(
            text = "常用漢字",
            fontSize = 30.sp,
            fontFamily = kouzanBrush,
            fontWeight = FontWeight.Bold,
            color = if (isSystemInDarkTheme()) Color.White else Color.Black
        )
    }
}