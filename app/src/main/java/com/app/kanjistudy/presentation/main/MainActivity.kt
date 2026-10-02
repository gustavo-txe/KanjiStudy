package com.app.kanjistudy.presentation.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.app.kanjistudy.core.ui.UserMessage
import com.app.kanjistudy.presentation.backup.AutoBackupNotice
import com.app.kanjistudy.data.preferences.OnboardingManager
import com.app.kanjistudy.data.review.InAppReviewManager
import com.app.kanjistudy.presentation.navigation.AppNavigation
import com.app.kanjistudy.presentation.theme.KanjiStudyTheme
import com.app.kanjistudy.presentation.theme.ThemeViewModel
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var inAppReviewManager: InAppReviewManager

    @Inject
    lateinit var onboardingManager: OnboardingManager

    private var sessionStartTimeMs: Long = 0L
    private var reviewJob: Job? = null
    private var keepSplashOnScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }
        lifecycleScope.launch {
            delay(2_000L)
            keepSplashOnScreen = false
        }
        enableEdgeToEdge()

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                sessionStartTimeMs = System.currentTimeMillis()
                lifecycleScope.launch {
                    inAppReviewManager.onAppSessionStarted(sessionStartTimeMs)
                }

                reviewJob?.cancel()
                reviewJob = lifecycleScope.launch {
                    delay(3 * 60 * 1000L)
                    val sessionDuration = System.currentTimeMillis() - sessionStartTimeMs
                    inAppReviewManager.maybeRequestReview(this@MainActivity, sessionDuration)
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                reviewJob?.cancel()
            }
        })

        setContent {
            val themeViewModel: ThemeViewModel = hiltViewModel()
            val isDarkTheme = themeViewModel.isDarkTheme.collectAsStateWithLifecycle()
            val userMessage = themeViewModel.userMessage.collectAsStateWithLifecycle()

            KanjiStudyTheme(darkTheme = isDarkTheme.value) {
                AutoBackupNotice()
                UserMessage(userMessage.value, themeViewModel::clearUserMessage)
                AppNavigation(
                    onboardingManager = onboardingManager,
                    isDarkTheme = isDarkTheme.value,
                    onThemeChanged = themeViewModel::onThemeChanged
                )
            }
        }
    }
}
