package com.app.kanjistudy.ui

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.app.kanjistudy.presentation.home.components.SearchBarKanji
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class SearchBarTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `typing updates query and search is submitted by button or keyboard`() {
        val query = mutableStateOf("")
        val submissions = mutableListOf<String>()
        compose.setContent {
            MaterialTheme {
                SearchBarKanji(query.value, { query.value = it }, { submissions.add(query.value) })
            }
        }
        compose.onNode(hasSetTextAction()).performTextInput("sun")
        compose.runOnIdle {
            assertEquals("sun", query.value)
            assertEquals(emptyList<String>(), submissions)
        }
        compose.onNodeWithContentDescription("Confirm search").performClick()
        compose.onNode(hasSetTextAction()).performImeAction()
        compose.runOnIdle { assertEquals(listOf("sun", "sun"), submissions) }
    }
}
