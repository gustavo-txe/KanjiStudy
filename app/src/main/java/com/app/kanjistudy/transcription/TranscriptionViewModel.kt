package com.app.kanjistudy.transcription

import android.app.Application
import android.media.MediaMetadataRetriever
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.data.repository.KanjiRepository
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import org.json.JSONObject
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import org.vosk.Model
import org.vosk.Recognizer

@HiltViewModel
class TranscriptionViewModel @Inject constructor(
    app: Application,
    private val repository: KanjiRepository
) : AndroidViewModel(app) {

    private val _uiState = MutableStateFlow(TranscriptionUiState())
    val uiState: StateFlow<TranscriptionUiState> = _uiState.asStateFlow()

    private val installer = ModelInstaller(app.applicationContext)
    private val decoder = AudioDecoder(app.applicationContext)
    private var translator: Translator? = null

    override fun onCleared() {
        translator?.close()
        super.onCleared()
    }

    fun initialize() { /* unchanged */
        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingPrerequisites = true) }
            if (!repository.isKanjiDownloadComplete()) {
                _uiState.update {
                    it.copy(isCheckingPrerequisites = false, prerequisiteMessage = "Please wait while the kanji download is complete.")
                }
                return@launch
            }
            val result = installer.ensureInstalled()
            _uiState.update { it.copy(isCheckingPrerequisites = false, isModelReady = result.isSuccess, error = result.exceptionOrNull()?.message) }
        }
    }

    fun reinstallModel() { viewModelScope.launch {
        _uiState.update { it.copy(isInstallingModel = true, error = null) }
        val result = installer.reinstall()
        _uiState.update { it.copy(isInstallingModel = false, isModelReady = result.isSuccess, error = result.exceptionOrNull()?.message) }
    } }

    fun onMediaSelected(uri: Uri) { viewModelScope.launch {
        val data = withContext(Dispatchers.IO) {
            MediaMetadataRetriever().use {
                it.setDataSource(getApplication(), uri)
                val duration = it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
                val hasVideo = (it.extractMetadata(MediaMetadataRetriever.METADATA_KEY_HAS_VIDEO) == "yes")
                duration to hasVideo
            }
        }
        _uiState.update {
            it.copy(selectedUri = uri, isVideoFile = data.second, mediaDurationMs = data.first, clipStartMs = 0L, clipEndMs = minOf(data.first, 120_000L), transcript = "", originalTranscript = "", translatedTranscript = "", isTranslatedView = false, error = null)
        }
    } }

    fun onClipChange(startMs: Long, endMs: Long) { _uiState.update { it.copy(clipStartMs = startMs, clipEndMs = endMs) } }

    fun toggleTranslation() {
        val state = _uiState.value
        if (state.isTranslating || state.originalTranscript.isBlank()) return

        if (state.isTranslatedView) {
            _uiState.update { it.copy(transcript = it.originalTranscript, isTranslatedView = false) }
            return
        }

        if (state.translatedTranscript.isNotBlank()) {
            _uiState.update { it.copy(transcript = it.translatedTranscript, isTranslatedView = true) }
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _uiState.update { it.copy(isTranslating = true, error = null) }
            runCatching {
                val tr = getTranslator()
                tr.downloadModelIfNeeded(DownloadConditions.Builder().build()).awaitVoid()
                tr.translate(state.originalTranscript).awaitString()
            }.onSuccess { translated ->
                _uiState.update { it.copy(isTranslating = false, translatedTranscript = translated, transcript = translated, isTranslatedView = true) }
            }.onFailure {
                _uiState.update { it.copy(isTranslating = false, error = "Translation failed. Please try again.") }
            }
        }
    }

    private fun getTranslator(): Translator {
        translator?.let { return it }
        val options = TranslatorOptions.Builder()
            .setSourceLanguage(TranslateLanguage.JAPANESE)
            .setTargetLanguage(TranslateLanguage.ENGLISH)
            .build()
        return Translation.getClient(options).also { translator = it }
    }

    fun transcribe() { /* mostly unchanged */
        val state = _uiState.value
        val uri = state.selectedUri ?: return
        if (!state.isModelReady || state.isTranscribing) return
        viewModelScope.launch(Dispatchers.Default) {
            _uiState.update { it.copy(isTranscribing = true, transcript = "", originalTranscript = "", translatedTranscript = "", isTranslatedView = false, error = null) }
            runCatching {
                val pcm = decoder.decodeToPcm16kMono(uri, state.clipStartMs, state.clipEndMs)
                val model = Model(installer.ensureInstalled().getOrThrow().absolutePath)
                val recognizer = Recognizer(model, 16000.0f)
                val chunkSamples = 2048
                val longPauseChunks = 8
                val energyJumpFactor = 2.2
                var silentChunks = 0
                var previousEnergy = 0.0
                val paragraphBuilder = StringBuilder()
                var idx = 0
                while (idx < pcm.size) {
                    val size = minOf(chunkSamples, pcm.size - idx)
                    val bytes = ByteArray(size * 2)
                    var energy = 0.0
                    for (i in 0 until size) {
                        val sample = pcm[idx + i].toInt(); energy += abs(sample).toDouble()
                        bytes[i * 2] = (sample and 0xff).toByte(); bytes[i * 2 + 1] = ((sample shr 8) and 0xff).toByte()
                    }
                    energy /= size.coerceAtLeast(1)
                    val isFinalized = recognizer.acceptWaveForm(bytes, bytes.size)
                    val isSilent = energy < 500
                    silentChunks = if (isSilent) silentChunks + 1 else 0
                    val highEnergyShift = previousEnergy > 0.0 && energy / previousEnergy > energyJumpFactor
                    if (isFinalized) appendText(paragraphBuilder, parseText(recognizer.result), silentChunks >= longPauseChunks || highEnergyShift)
                    previousEnergy = if (energy > 0) energy else previousEnergy
                    idx += size
                }
                appendText(paragraphBuilder, parseText(recognizer.finalResult), false)
                recognizer.close(); model.close(); paragraphBuilder.toString().trim()
            }.onSuccess { result ->
                val finalText = result.ifBlank { "No speech was recognized." }
                _uiState.update { it.copy(isTranscribing = false, transcript = finalText, originalTranscript = finalText) }
            }.onFailure {
                _uiState.update { st -> st.copy(isTranscribing = false, error = it.message) }
            }
        }
    }

    private fun parseText(json: String): String = runCatching { JSONObject(json).optString("text").trim() }.getOrDefault("")
    private fun appendText(builder: StringBuilder, text: String, paragraphBreak: Boolean) { if (text.isNotBlank()) { if (builder.isNotEmpty()) builder.append(if (paragraphBreak) "\n\n" else " "); builder.append(text) } }
}

private suspend fun com.google.android.gms.tasks.Task<Void>.awaitVoid() = suspendCancellableCoroutine<Unit> { c ->
    addOnSuccessListener { if (c.isActive) c.resume(Unit) {} }
    addOnFailureListener { if (c.isActive) c.resumeWithException(it) }
}

private suspend fun com.google.android.gms.tasks.Task<String>.awaitString() = suspendCancellableCoroutine<String> { c ->
    addOnSuccessListener { if (c.isActive) c.resume(it) {} }
    addOnFailureListener { if (c.isActive) c.resumeWithException(it) }
}