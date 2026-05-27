package com.app.kanjistudy.home.kanjis

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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
            SharingStarted.Companion.WhileSubscribed(5000),
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
                    val joyoKanjis = localData.map { kanjiData -> kanjiData.kanji }
                    it.copy(
                        joyoKanjis = joyoKanjis,
                        isLoading = false,
                        loadingProgress = 1f,
                        filteredKanjis = applyFilters(it.copy(joyoKanjis = joyoKanjis))
                    )
                }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = "Erro ao carregar kanjis"
                    )
                }
            }

        }
    }

    private fun updateMaps(data: List<KanjiData>) {
        _uiState.update {
            it.copy(
                kunReadings = data.associate { it.kanji to it.kunReadings },
                onReadings = data.associate { it.kanji to it.onReadings },
                meanings = data.associate { it.kanji to it.meanings },
                jlptLevels = data.associate { it.kanji to it.jlpt }
            )
        }
    }

    fun onQueryChange(query: String) {
        _uiState.update { state ->
            val updatedState = state.copy(query = query)
            updatedState.copy(filteredKanjis = applyFilters(updatedState))
        }
    }

    fun onJlptLevelSelected(level: Int?) {
        _uiState.update { state ->
            val updatedState = state.copy(selectedJlptLevel = level)
            updatedState.copy(filteredKanjis = applyFilters(updatedState))
        }
    }

    private fun applyFilters(state: KanjiUiState): List<String> {
        val normalizedQuery = state.query.trim().lowercase()
        return state.joyoKanjis.filter { kanji ->
            val levelMatches = state.selectedJlptLevel == null || state.jlptLevels[kanji] == state.selectedJlptLevel
            val queryMatches = normalizedQuery.isBlank() || matchesKanji(state, kanji, normalizedQuery)
            levelMatches && queryMatches
        }
    }

    private fun matchesKanji(state: KanjiUiState, kanji: String, query: String): Boolean {
        if (kanji == query) return true

        val readingMatches = state.kunReadings[kanji].orEmpty().any { exactTermMatch(it, query) } ||
                state.onReadings[kanji].orEmpty().any { exactTermMatch(it, query) }
        val meaningMatches = state.meanings[kanji].orEmpty().any { exactTermMatch(it, query) }

        return readingMatches || meaningMatches
    }

    private fun exactTermMatch(value: String, query: String): Boolean {
        val normalized = value.trim().lowercase()
        if (normalized == query) return true
        return normalized
            .split(Regex("[\\s,;:.!?()\\[\\]{}\\-_/、。・]+"))
            .filter { it.isNotBlank() }
            .any { it == query }
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