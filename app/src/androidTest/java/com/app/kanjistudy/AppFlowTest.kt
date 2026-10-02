package com.app.kanjistudy

import android.app.Activity
import android.app.Instrumentation.ActivityResult
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextReplacement
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.intent.Intents
import androidx.test.espresso.intent.Intents.intended
import androidx.test.espresso.intent.Intents.intending
import androidx.test.espresso.intent.matcher.IntentMatchers.hasAction
import androidx.test.espresso.intent.matcher.IntentMatchers.hasExtra
import androidx.test.espresso.intent.matcher.IntentMatchers.hasType
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.filters.SdkSuppress
import com.app.kanjistudy.data.preferences.AppPreferencesRepository
import com.app.kanjistudy.data.repository.KanjiRepository
import com.app.kanjistudy.presentation.main.MainActivity
import com.app.kanjistudy.testing.TestStorage
import com.google.gson.JsonParser
import dagger.hilt.android.testing.BindValue
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import java.util.UUID
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.hamcrest.Matchers.allOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@HiltAndroidTest
@LargeTest
@RunWith(AndroidJUnit4::class)
class AppFlowTest {
    @get:Rule(order = 0) val hilt = HiltAndroidRule(this)
    @get:Rule(order = 1) val compose = createEmptyComposeRule()

    @BindValue lateinit var repository: KanjiRepository
    @BindValue lateinit var preferences: AppPreferencesRepository

    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var storage: TestStorage
    private var scenario: ActivityScenario<MainActivity>? = null
    private val documents = mutableListOf<Uri>()

    @Before fun setUp() {
        storage = TestStorage(context)
        runBlocking { storage.seedCatalog() }
        repository = storage.repository
        preferences = storage.preferences
        hilt.inject()
        Intents.init()
        launchApp()
    }

    @After fun tearDown() {
        scenario?.close()
        Intents.release()
        documents.forEach { context.contentResolver.delete(it, null, null) }
        if (::storage.isInitialized) storage.close()
    }

    @Test fun searchMarkRecreateAndRemoveLearnedKanji() {
        searchFor("sun")
        compose.onNodeWithText("日").assertIsDisplayed()
        compose.onNodeWithContentDescription("Learned status").performClick()
        compose.onNodeWithText("Add Kanji?").assertIsDisplayed()
        compose.onNodeWithText("Yes").performClick()
        awaitLearned(setOf("日"))

        compose.onNodeWithText("Learned").performClick()
        awaitText("1/2136 Jōyō kanji learned")
        compose.onNodeWithText("日").assertIsDisplayed()

        scenario!!.recreate()
        awaitText("1/2136 Jōyō kanji learned")
        compose.onNodeWithText("日").performClick()
        compose.onNodeWithContentDescription("Remove Kanji").performClick()
        compose.onNodeWithText("Yes").performClick()
        awaitLearned(emptySet())
        awaitText("You haven't marked any learned kanji yet.")
        compose.onNodeWithText("0/2136 Jōyō kanji learned").assertIsDisplayed()
    }

    @Test fun themeChoiceSurvivesClosingAndRelaunchingActivity() {
        compose.onNodeWithContentDescription("Open menu").performClick()
        compose.onNodeWithText("Dark theme").performClick()
        awaitText("Light theme")
        runBlocking { withTimeout(5_000) { preferences.isDarkTheme.first { !it } } }

        scenario!!.close()
        launchApp()
        compose.onNodeWithContentDescription("Open menu").performClick()
        awaitText("Light theme")
        assertFalse(runBlocking { preferences.isDarkTheme.first() })
    }

    @Test fun helpCanBeOpenedFromDrawerAndBackReturnsToSearch() {
        searchFor("moon")
        compose.onNodeWithContentDescription("Open menu").performClick()
        compose.onNodeWithText("Help").performClick()
        compose.onNodeWithContentDescription("Back").assertIsDisplayed().performClick()
        awaitText("月")
        compose.onNode(hasSetTextAction()).assertTextContains("moon")
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun exportAndImportThroughDocumentResultsPreserveAndMergeProgress() {
        runBlocking { repository.toggleLearnedKanji("日") }
        val uri = newDocument()
        intending(hasAction(Intent.ACTION_CREATE_DOCUMENT)).respondWith(
            ActivityResult(Activity.RESULT_OK, Intent().setData(uri))
        )
        openBackup()
        compose.onNodeWithText("Export JSON").performScrollTo().performClick()
        awaitText("Learned kanji exported successfully.")
        intended(allOf(hasAction(Intent.ACTION_CREATE_DOCUMENT), hasType("application/json"), hasExtra(Intent.EXTRA_TITLE, "learned-kanji.json")))
        val json = context.contentResolver.openInputStream(uri)!!.bufferedReader(Charsets.UTF_8).use { it.readText() }
        assertEquals(listOf("日"), JsonParser.parseString(json).asJsonObject["learnedKanji"].asJsonArray.map { it.asString })

        runBlocking {
            repository.toggleLearnedKanji("日")
            repository.toggleLearnedKanji("月")
        }
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(
            ActivityResult(Activity.RESULT_OK, Intent().setData(uri))
        )
        compose.onNodeWithText("Import JSON").performScrollTo().performClick()
        awaitLearned(setOf("日", "月"))
        intended(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Learned").performClick()
        awaitText("2/2136 Jōyō kanji learned")
        compose.onNodeWithText("日").assertIsDisplayed()
        compose.onNodeWithText("月").assertIsDisplayed()
    }

    @Test fun cancellingDocumentSelectionLeavesProgressUnchangedAndDialogUsable() {
        runBlocking { repository.toggleLearnedKanji("日") }
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(ActivityResult(Activity.RESULT_CANCELED, null))
        openBackup()
        compose.onNodeWithText("Import JSON").performScrollTo().performClick()
        compose.waitForIdle()
        intended(hasAction(Intent.ACTION_OPEN_DOCUMENT))
        assertEquals(listOf("日"), runBlocking { storage.dao.getLearnedKanjisOnce().map { it.kanji } })
        compose.onNodeWithText("Close").performClick()
        compose.onNodeWithText("Learned").performClick()
        awaitText("1/2136 Jōyō kanji learned")
    }

    @Test
    @SdkSuppress(minSdkVersion = 29)
    fun invalidDocumentShowsErrorWithoutChangingLearnedProgress() {
        runBlocking { repository.toggleLearnedKanji("日") }
        val uri = newDocument()
        context.contentResolver.openOutputStream(uri, "wt")!!.use { it.write("[null]".toByteArray()) }
        intending(hasAction(Intent.ACTION_OPEN_DOCUMENT)).respondWith(ActivityResult(Activity.RESULT_OK, Intent().setData(uri)))
        openBackup()
        compose.onNodeWithText("Import JSON").performScrollTo().performClick()
        awaitText("Invalid backup. Choose a JSON file containing a list of kanji strings.")
        assertEquals(listOf("日"), runBlocking { storage.dao.getLearnedKanjisOnce().map { it.kanji } })
        compose.onNodeWithText("Close").performClick()
    }

    private fun launchApp() {
        scenario = ActivityScenario.launch(MainActivity::class.java)
        awaitText("sun")
    }

    private fun searchFor(query: String) {
        compose.onNode(hasSetTextAction()).performTextReplacement(query)
        compose.onNodeWithContentDescription("Confirm search").performClick()
        compose.waitForIdle()
    }

    private fun openBackup() {
        compose.onNodeWithContentDescription("Open menu").performClick()
        compose.onNodeWithText("Import / Export").performClick()
        awaitText("Learned Kanji Backup")
    }

    private fun awaitText(text: String) {
        compose.waitUntil(timeoutMillis = 10_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun awaitLearned(expected: Set<String>) = runBlocking {
        withTimeout(5_000) {
            storage.dao.getLearnedKanjis().first { entries -> entries.map { it.kanji }.toSet() == expected }
        }
    }

    @android.annotation.TargetApi(29)
    private fun newDocument(): Uri {
        val values = ContentValues().apply {
            put(MediaStore.Downloads.DISPLAY_NAME, "kanji-test-${UUID.randomUUID()}.json")
            put(MediaStore.Downloads.MIME_TYPE, "application/json")
            put(MediaStore.Downloads.RELATIVE_PATH, "${Environment.DIRECTORY_DOWNLOADS}/KanjiStudyTests")
            put(MediaStore.Downloads.IS_PENDING, 1)
        }
        return checkNotNull(context.contentResolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values))
            .also(documents::add)
    }
}
