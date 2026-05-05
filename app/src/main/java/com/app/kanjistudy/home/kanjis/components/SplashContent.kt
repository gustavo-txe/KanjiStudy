package com.app.kanjistudy.home.kanjis.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.sp
import com.app.kanjistudy.R
import com.app.kanjistudy.navigation.AppNavigation
import com.app.kanjistudy.onboarding.OnboardingManager
import com.app.kanjistudy.theme.KanjiStudyTheme
import kotlinx.coroutines.delay

@Composable
fun SplashContent(onboardingManager: OnboardingManager) {
    var showSplash by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        delay(2_000L)
        showSplash = false
    }

    if (showSplash) {
        val kouzanBrush = FontFamily(Font(R.font.kouzanbrush))

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "字",
                style = TextStyle(
                    color = Color.White,
                    fontSize = 150.sp,
                    fontFamily = kouzanBrush
                )
            )
        }
    } else {
        KanjiStudyTheme {
            AppNavigation(onboardingManager = onboardingManager)
        }
    }
}