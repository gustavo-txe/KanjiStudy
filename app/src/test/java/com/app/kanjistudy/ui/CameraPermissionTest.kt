package com.app.kanjistudy.ui

import android.Manifest
import android.app.Application
import android.provider.Settings
import androidx.activity.compose.LocalActivityResultRegistryOwner
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.ActivityResultRegistryOwner
import androidx.activity.result.contract.ActivityResultContract
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.core.app.ActivityOptionsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.app.kanjistudy.presentation.scan.camera.CameraPermission
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [28])
class CameraPermissionTest {
    @get:Rule val compose = createComposeRule()

    @Test fun `denial keeps image scanning available and settings grant restores camera without repeated prompts`() {
        val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this)
        }
        var requests = 0
        var requestCode = 0
        var imageScans = 0
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                code: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?,
            ) {
                assertEquals(Manifest.permission.CAMERA, input)
                requests++
                requestCode = code
            }
        }
        val registryOwner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = registry
        }
        compose.setContent {
            CompositionLocalProvider(
                LocalLifecycleOwner provides owner,
                LocalActivityResultRegistryOwner provides registryOwner,
            ) {
                MaterialTheme {
                    CameraPermission(onScanImage = { imageScans++ }) {
                        Text("Camera ready")
                    }
                }
            }
        }
        compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        compose.runOnIdle {
            assertEquals(1, requests)
            // The permission dialog pauses/resumes the screen without a new entry.
            owner.lifecycle.currentState = Lifecycle.State.STARTED
            registry.dispatchResult(requestCode, false)
            owner.lifecycle.currentState = Lifecycle.State.RESUMED
        }
        compose.runOnIdle {
            assertEquals(1, requests)
        }
        compose.onNodeWithText("Camera permission is required to scan kanji.").assertIsDisplayed()
        compose.onNodeWithText("Enable camera permission").assertIsDisplayed().performClick()
        compose.runOnIdle {
            val settingsIntent = shadowOf(RuntimeEnvironment.getApplication()).nextStartedActivity
            assertEquals(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, settingsIntent.action)
            assertEquals("package:${RuntimeEnvironment.getApplication().packageName}", settingsIntent.dataString)
        }
        compose.onNodeWithText("Scan Image").assertIsDisplayed().performClick()
        compose.runOnIdle {
            assertEquals(1, imageScans)
            owner.lifecycle.currentState = Lifecycle.State.CREATED
            owner.lifecycle.currentState = Lifecycle.State.RESUMED
        }
        compose.runOnIdle {
            assertEquals(1, requests)
            owner.lifecycle.currentState = Lifecycle.State.CREATED
            shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.CAMERA)
            owner.lifecycle.currentState = Lifecycle.State.RESUMED
        }
        compose.runOnIdle { assertEquals(1, requests) }
        compose.onNodeWithText("Camera ready").assertIsDisplayed()
        compose.onNodeWithText("Enable camera permission").assertDoesNotExist()
    }

    @Test fun `already granted permission shows camera without requesting permission`() {
        shadowOf(RuntimeEnvironment.getApplication()).grantPermissions(Manifest.permission.CAMERA)
        val owner = object : LifecycleOwner {
            override val lifecycle = LifecycleRegistry.createUnsafe(this)
        }
        val registryOwner = object : ActivityResultRegistryOwner {
            override val activityResultRegistry = object : ActivityResultRegistry() {
                override fun <I, O> onLaunch(
                    code: Int,
                    contract: ActivityResultContract<I, O>,
                    input: I,
                    options: ActivityOptionsCompat?,
                ) {
                    throw AssertionError("Granted permission must not be requested again")
                }
            }
        }
        compose.setContent {
            CompositionLocalProvider(
                LocalLifecycleOwner provides owner,
                LocalActivityResultRegistryOwner provides registryOwner,
            ) {
                MaterialTheme {
                    CameraPermission(onScanImage = {}) { Text("Camera ready") }
                }
            }
        }
        compose.runOnIdle { owner.lifecycle.currentState = Lifecycle.State.RESUMED }
        compose.onNodeWithText("Camera ready").assertIsDisplayed()
        compose.onNodeWithText("Enable camera permission").assertDoesNotExist()
    }
}
