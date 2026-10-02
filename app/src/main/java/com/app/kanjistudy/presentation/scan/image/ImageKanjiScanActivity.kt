package com.app.kanjistudy.presentation.scan.image

import android.os.Bundle
import com.app.kanjistudy.core.ui.UserMessage
import com.app.kanjistudy.presentation.backup.AutoBackupNotice
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.kanjistudy.data.preferences.OnboardingManager
import com.app.kanjistudy.presentation.theme.KanjiStudyTheme
import com.app.kanjistudy.presentation.theme.ThemeViewModel
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
                AutoBackupNotice()
                UserMessage(themeViewModel.userMessage.collectAsStateWithLifecycle().value, themeViewModel::clearUserMessage)
                ImageScanScreen(
                    onboardingManager = onboardingManager,
                    isDarkTheme = isDarkTheme.value,
                    onThemeChanged = themeViewModel::onThemeChanged
                )
            }
        }
    }
}
