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
                    howToUse = "Use the search bar. Tap a kanji to get details via Google AI or copy it to the clipboard. Finally, use the check icon to mark it as learned."
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
                            "(Stylized kanji, decorative fonts or image quality can make identification more difficult)."
                ),
                HelpFeature(
                    title = "Learned",
                    description = "Track all kanji you already learned.",
                    howToUse = "Open Learned tab, check your progress bar, tap a kanji for details, and remove it when needed."
                ),
                HelpFeature(
                    title = "Import / Export Learned Kanji",
                    description = "Back up your learned kanji progress or restore it on this device.",
                    howToUse = "Open the top-right menu and select Import / Export. Tap Export JSON to save a learned-kanji.json backup, or Import JSON to restore one. Imported kanji are merged with your current learned list."
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