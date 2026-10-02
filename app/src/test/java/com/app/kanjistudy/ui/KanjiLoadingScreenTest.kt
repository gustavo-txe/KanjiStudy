package com.app.kanjistudy.ui

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.app.kanjistudy.R
import com.app.kanjistudy.core.ui.UiText
import com.app.kanjistudy.presentation.home.KanjiUiState
import com.app.kanjistudy.presentation.home.components.KanjiLoadingScreen
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class KanjiLoadingScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `error offers a working retry action`() {
        var retries = 0
        compose.setContent {
            MaterialTheme {
                KanjiLoadingScreen(
                    uiState = KanjiUiState(isLoading = false, error = UiText(R.string.catalog_load_error)),
                    onRetry = { retries++ }
                )
            }
        }
        compose.onNodeWithText("Unable to load kanji. Check your connection and try again.").assertIsDisplayed()
        compose.onNodeWithText("Try again").performClick()
        compose.runOnIdle { assertEquals(1, retries) }
    }
}
