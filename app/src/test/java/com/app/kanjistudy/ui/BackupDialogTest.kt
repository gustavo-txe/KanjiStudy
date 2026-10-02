package com.app.kanjistudy.ui

import android.app.Application
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import com.app.kanjistudy.presentation.navigation.LearnedKanjiBackupDialog
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28], qualifiers = "w320dp-h480dp")
class BackupDialogTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `ready dialog routes import export and close actions`() {
        var imports = 0
        var exports = 0
        var dismissals = 0
        compose.setContent {
            MaterialTheme {
                LearnedKanjiBackupDialog(false, false, null, { dismissals++ }, { imports++ }, { exports++ })
            }
        }
        compose.onNodeWithText("Import JSON").performClick()
        compose.onNodeWithText("Export JSON").performClick()
        compose.onNodeWithText("Close").performClick()
        compose.runOnIdle {
            assertEquals(1, imports)
            assertEquals(1, exports)
            assertEquals(1, dismissals)
        }
    }

    @Test fun `busy dialog disables transfer and close and shows progress message`() {
        compose.setContent {
            MaterialTheme {
                LearnedKanjiBackupDialog(true, false, "Importing…", {}, {}, {})
            }
        }
        compose.onNodeWithText("Import JSON").assertIsNotEnabled()
        compose.onNodeWithText("Export JSON").assertIsNotEnabled()
        compose.onNodeWithText("Close").assertIsNotEnabled()
        compose.onNodeWithText("Importing…").performScrollTo().assertIsDisplayed()
    }

    @Test fun `downloading kanji disables transfers but allows closing`() {
        compose.setContent {
            MaterialTheme { LearnedKanjiBackupDialog(false, true, null, {}, {}, {}) }
        }
        compose.onNodeWithText("Import JSON").assertIsNotEnabled()
        compose.onNodeWithText("Export JSON").assertIsNotEnabled()
        compose.onNodeWithText("Close").assertIsEnabled()
    }
}
