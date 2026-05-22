package com.app.kanjistudy.help

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HelpViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        HelpUiState(
            features = listOf(
                HelpFeature(
                    title = "Kanji List",
                    description = "Explore all Jōyō kanji with meanings and readings.",
                    howToUse = "Use the search bar, tap a kanji to open Google search, and use the add/check icon to mark as learned."
                ),
                HelpFeature(
                    title = "Scan (Camera)",
                    description = "Recognize kanji in real-time using your camera.",
                    howToUse = "Point the camera to Japanese text, tap a detected kanji for options " +
                            "(Stylized kanji or decorative fonts can make identification more difficult)."
                ),
                HelpFeature(
                    title = "Scan (Image)",
                    description = "Recognize kanji from an image in your gallery.",
                    howToUse = "Open the image scan button in the scan screen, select a photo and review extracted kanji " +
                            "(Stylized kanji or decorative fonts can make identification more difficult)."
                ),
                HelpFeature(
                    title = "Learned",
                    description = "Track all kanji you already learned.",
                    howToUse = "Open Learned tab, check your progress bar, tap a kanji for details, and remove it when needed."
                ),
                HelpFeature(
                    title = "Theme",
                    description = "Switch between light and dark mode.",
                    howToUse = "Open the top-right menu and toggle Dark theme switch."
                )
            )
        )
    )

    val uiState: StateFlow<HelpUiState> = _uiState.asStateFlow()
}