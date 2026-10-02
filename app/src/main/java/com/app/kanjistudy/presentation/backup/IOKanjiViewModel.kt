package com.app.kanjistudy.presentation.backup

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.R
import com.app.kanjistudy.core.ui.UiText
import com.app.kanjistudy.core.ui.toProgressErrorMessage
import com.app.kanjistudy.data.document.JsonDocumentStore
import com.app.kanjistudy.data.repository.KanjiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class IOKanjiViewModel @Inject constructor(
    private val documents: JsonDocumentStore,
    private val repository: KanjiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(IOKanjiUiState())
    val uiState = _uiState.asStateFlow()

    fun exportLearnedKanjis(uri: Uri) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, busyMessage = UiText(R.string.export_in_progress), userMessage = null) }

        viewModelScope.launch {

            val result = runCatching {
                val json = repository.exportLearnedKanjisJson()
                documents.write(uri, json)
            }.onFailure { if (it is CancellationException) throw it }

            _uiState.update {
                it.copy(
                    isBusy = false,
                    busyMessage = null,
                    userMessage = result.fold(
                        onSuccess = { UiText(R.string.export_success) },
                        onFailure = { error -> error.toProgressErrorMessage(R.string.export_error) }
                    )
                )
            }
        }
    }

    fun importLearnedKanjis(uri: Uri) {
        if (_uiState.value.isBusy) return
        _uiState.update { it.copy(isBusy = true, busyMessage = UiText(R.string.import_in_progress), userMessage = null) }

        viewModelScope.launch {

            val result = runCatching {
                val json = documents.read(uri)
                repository.importLearnedKanjisJson(json)
            }.onFailure { if (it is CancellationException) throw it }

            _uiState.update {
                it.copy(
                    isBusy = false,
                    busyMessage = null,
                    userMessage = result.fold(
                        onSuccess = { importResult ->
                            UiText(R.string.import_success, listOf(importResult.importedCount, importResult.skippedCount))
                        },
                        onFailure = { error ->
                            error.toProgressErrorMessage(
                                if (error is IllegalArgumentException) R.string.import_invalid else R.string.import_error
                            )
                        }
                    )
                )
            }
        }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }
}
