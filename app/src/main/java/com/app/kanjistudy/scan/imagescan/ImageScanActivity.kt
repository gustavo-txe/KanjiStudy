package com.app.kanjistudy.scan.imagescan

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.kanjistudy.onboarding.OnboardingManager
import com.app.kanjistudy.theme.KanjiStudyTheme
import com.app.kanjistudy.theme.ThemeViewModel
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
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkTheme = themeViewModel.isDarkTheme.collectAsStateWithLifecycle()

            KanjiStudyTheme(darkTheme = isDarkTheme.value) {
                ImageScanScreen(
                    onboardingManager = onboardingManager,
                    isDarkTheme = isDarkTheme.value,
                    onThemeChanged = themeViewModel::onThemeChanged
                )
            }
        }
    }
}

