package com.app.kanjistudy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.KanjiDialog
import com.app.kanjistudy.KanjiUiState
import com.app.kanjistudy.KanjiUiEvent
import com.app.kanjistudy.data.model.KanjiData
import com.app.kanjistudy.data.repository.KanjiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class KanjiViewModel @Inject constructor(
    private val repository: KanjiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(KanjiUiState())
    val uiState: StateFlow<KanjiUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<KanjiUiEvent>()
    val uiEvent: SharedFlow<KanjiUiEvent?> = _uiEvent.asSharedFlow()

    val learnedKanjis: StateFlow<List<KanjiData>> = repository.getLearnedKanjis()
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5000),
            emptyList()
        )

    init {
        loadKanjis()
    }

    fun markLearnedKanji(kanji: String) {
        viewModelScope.launch {
            repository.toggleLearnedKanji(kanji)
        }
    }

    private fun loadKanjis() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            try {
                val localData = repository.ensureAllKanjisLoaded(
                    onProgress = { progress ->
                        _uiState.update { it.copy(loadingProgress = progress) }
                    }
                )
                updateMaps(localData)

                _uiState.update {
                    it.copy(
                        joyoKanjis = localData.map { it.kanji },
                        isLoading = false,
                        loadingProgress = 1f,
                        filteredKanjis = localData.map { it.kanji }
                    )
                }

            } catch (e: Exception) {
                _uiState.update { it.copy(error = "Erro ao carregar kanjis") }
            }

        }
    }

    private fun updateMaps(data: List<KanjiData>) {
        _uiState.update {
            it.copy(
                kunReadings = data.associate { it.kanji to it.kunReadings },
                onReadings = data.associate { it.kanji to it.onReadings },
                meanings = data.associate { it.kanji to it.meanings }
            )
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { state ->
            state.copy(
                query = query,
                filteredKanjis = filterKanjis(query, state)
            )
        }
    }

    private fun filterKanjis(
        query: String,
        state: KanjiUiState
    ): List<String> {

        val lowerQuery = query.trim().lowercase()

        if (lowerQuery.isBlank()) {
            return state.joyoKanjis
        }

        return state.joyoKanjis.filter { kanji ->
            val kun = state.kunReadings[kanji]
                ?.joinToString(" ")
                ?.lowercase()
                .orEmpty()

            val on = state.onReadings[kanji]
                ?.joinToString(" ")
                ?.lowercase()
                .orEmpty()

            val mean = state.meanings[kanji]
                ?.joinToString(" ")
                ?.lowercase()
                .orEmpty()

            kanji.contains(lowerQuery) ||
                    kun.contains(lowerQuery) ||
                    on.contains(lowerQuery) ||
                    mean.contains(lowerQuery)
        }
    }

    fun addLearnedKanji(kanji: String, isLearned: Boolean) {
        viewModelScope.launch {
            _uiEvent.emit(
                KanjiUiEvent.ShowDialog(
                    KanjiDialog.ToggleLearned(
                        kanji = kanji,
                        isLearned = isLearned
                    )
                )
            )
        }
    }

    fun openGoogleSearch(kanji: String) {
        viewModelScope.launch {
            _uiEvent.emit(
                KanjiUiEvent.ShowDialog(
                    KanjiDialog.Google(kanji)
                )
            )
        }
    }


}
