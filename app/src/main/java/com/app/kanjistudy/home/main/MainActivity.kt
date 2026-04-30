package com.app.kanjistudy.home.main

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.lifecycleScope
import com.app.kanjistudy.navigation.AppNavigation
import com.app.kanjistudy.review.InAppReviewManager
import com.app.kanjistudy.theme.KanjiStudyTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var inAppReviewManager: InAppReviewManager

    private var sessionStartTimeMs: Long = 0L
    private var reviewJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        installSplashScreen()
        enableEdgeToEdge()

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                sessionStartTimeMs = System.currentTimeMillis()
                inAppReviewManager.onAppSessionStarted(sessionStartTimeMs)

                reviewJob?.cancel()
                reviewJob = lifecycleScope.launch {
                    delay(10 * 1000L)
                    val sessionDuration = System.currentTimeMillis() - sessionStartTimeMs
                    inAppReviewManager.maybeRequestReview(this@MainActivity, sessionDuration)
                }
            }

            override fun onStop(owner: LifecycleOwner) {
                reviewJob?.cancel()
            }
        })

        setContent {
            KanjiStudyTheme {
                AppNavigation()
            }
        }
    }
}