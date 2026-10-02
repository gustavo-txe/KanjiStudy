package com.app.kanjistudy.presentation.theme

import androidx.lifecycle.ViewModelStore
import app.cash.turbine.test
import com.app.kanjistudy.data.preferences.ThemePreferenceRepository
import com.app.kanjistudy.testing.MainDispatcherRule
import io.mockk.*
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {
    @get:Rule val main = MainDispatcherRule()

    @Test fun `subscribed theme follows preferences and user choice is persisted`() = runTest {
        val preferences = mockk<ThemePreferenceRepository>()
        val dark = MutableStateFlow(false)
        every { preferences.isDarkTheme() } returns dark
        coEvery { preferences.setDarkTheme(true) } coAnswers { dark.value = true }
        val store = ViewModelStore()
        val vm = ThemeViewModel(preferences).also { store.put("theme", it) }
        try {
            vm.isDarkTheme.test {
                assertTrue(awaitItem())
                assertFalse(awaitItem())
                vm.onThemeChanged(true)
                assertTrue(awaitItem())
            }
            runCurrent()
            coVerify(exactly = 1) { preferences.setDarkTheme(true) }
        } finally { store.clear() }
    }
}
