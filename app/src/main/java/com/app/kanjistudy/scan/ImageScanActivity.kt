package com.app.kanjistudy.scan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.app.kanjistudy.onboarding.OnboardingManager
import com.app.kanjistudy.theme.KanjiStudyTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class ImageKanjiScanActivity : ComponentActivity() {

    @Inject
    lateinit var onboardingManager: OnboardingManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            KanjiStudyTheme {
                ImageScanScreen(onboardingManager = onboardingManager)
            }
        }
    }
}

