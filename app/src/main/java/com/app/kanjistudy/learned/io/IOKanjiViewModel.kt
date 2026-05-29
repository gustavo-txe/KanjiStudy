package com.app.kanjistudy.learned.io

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.data.repository.KanjiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class IOKanjiViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: KanjiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IOKanjiUiState())
    val uiState = _uiState.asStateFlow()

    fun exportLearnedKanjis(uri: Uri) {
        if (_uiState.value.isBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, busyMessage = "Exporting learned kanji…") }

            val result = runCatching {
                val json = repository.exportLearnedKanjisJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { outputStream ->
                        outputStream.write(json.toByteArray(Charsets.UTF_8))
                    } ?: throw IOException("Unable to open the selected file.")
                }
            }

            _uiState.update {
                it.copy(
                    isBusy = false,
                    busyMessage = null,
                    userMessage = result.fold(
                        onSuccess = { "Learned kanji exported successfully." },
                        onFailure = { error -> error.message ?: "Could not export learned kanji." }
                    )
                )
            }
        }
    }

    fun importLearnedKanjis(uri: Uri) {
        if (_uiState.value.isBusy) return

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, busyMessage = "Importing learned kanji…") }

            val result = runCatching {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                    } ?: throw IOException("Unable to open the selected file.")
                }
                repository.importLearnedKanjisJson(json)
            }

            _uiState.update {
                it.copy(
                    isBusy = false,
                    busyMessage = null,
                    userMessage = result.fold(
                        onSuccess = { importResult ->
                            "Imported ${importResult.importedCount} learned kanji. ${importResult.skippedCount} skipped."
                        },
                        onFailure = { error -> error.message ?: "Could not import learned kanji." }
                    )
                )
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}