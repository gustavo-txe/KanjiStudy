package com.app.kanjistudy.transcription

import android.net.Uri

data class TranscriptionUiState(
    val isCheckingPrerequisites: Boolean = false,
    val prerequisiteMessage: String? = null,
    val isInstallingModel: Boolean = false,
    val isModelReady: Boolean = false,
    val selectedUri: Uri? = null,
    val isVideoFile: Boolean = false,
    val mediaDurationMs: Long = 0L,
    val clipStartMs: Long = 0L,
    val clipEndMs: Long = 0L,
    val isTranscribing: Boolean = false,
    val transcript: String = "",
    val originalTranscript: String = "",
    val translatedTranscript: String = "",
    val isTranslatedView: Boolean = false,
    val isTranslating: Boolean = false,
    val error: String? = null
)