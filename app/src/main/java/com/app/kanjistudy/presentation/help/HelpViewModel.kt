package com.app.kanjistudy.presentation.help

import androidx.lifecycle.ViewModel
import com.app.kanjistudy.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class HelpViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        HelpUiState(
            features = listOf(
                HelpFeature(
                    title = R.string.kanji_list,
                    description = R.string.help_catalog_description,
                    howToUse = R.string.help_catalog_instructions
                ),
                HelpFeature(
                    title = R.string.scan_camera,
                    description = R.string.help_camera_description,
                    howToUse = R.string.help_camera_instructions
                ),
                HelpFeature(
                    title = R.string.help_image_title,
                    description = R.string.help_image_description,
                    howToUse = R.string.help_image_instructions
                ),
                HelpFeature(
                    title = R.string.learned,
                    description = R.string.help_learned_description,
                    howToUse = R.string.help_learned_instructions
                ),
                HelpFeature(
                    title = R.string.import_export_learned_kanji,
                    description = R.string.help_backup_description,
                    howToUse = R.string.help_backup_instructions
                ),
                HelpFeature(
                    title = R.string.theme,
                    description = R.string.help_theme_description,
                    howToUse = R.string.help_theme_instructions
                )
            )
        )
    )

    val uiState: StateFlow<HelpUiState> = _uiState.asStateFlow()
}
