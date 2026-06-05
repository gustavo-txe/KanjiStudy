package com.app.kanjistudy.learned

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.data.model.KanjiData
import com.app.kanjistudy.data.repository.KanjiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LearnedViewModel @Inject constructor(
    private val repository: KanjiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearnedUiState())
    val uiState = _uiState.asStateFlow()

    init {
        observeLearnedKanjis()
    }

    private fun observeLearnedKanjis() {
        viewModelScope.launch {
            repository.getLearnedKanjis().collect { kanjis ->
                _uiState.update { state ->
                    state.copy(
                        kanjis = kanjis,
                        filteredKanjis = applyFilters(kanjis, state.query, state.selectedJlptLevel)
                    )
                }
            }
        }
    }

    fun onJlptLevelSelected(level: Int?) {
        _uiState.update { state ->
            state.copy(
                selectedJlptLevel = level,
                filteredKanjis = applyFilters(state.kanjis, state.query, level)
            )
        }
    }

    private fun applyFilters(kanjis: List<KanjiData>, query: String, level: Int?): List<KanjiData> {
        val normalizedQuery = query.trim().lowercase()
        return kanjis.filter { kanji ->
            val levelMatches = level == null || kanji.jlpt == level
            val queryMatches = normalizedQuery.isBlank() || matchesKanji(kanji, normalizedQuery)
            levelMatches && queryMatches
        }
    }

    private fun matchesKanji(kanji: KanjiData, query: String): Boolean {
        if (kanji.kanji == query) return true
        return (kanji.kunReadings + kanji.onReadings + kanji.meanings).any {
            exactTermMatch(
                it,
                query
            )
        }
    }

    private fun exactTermMatch(value: String, query: String): Boolean {
        val normalized = value.trim().lowercase()
        if (normalized == query) return true
        return normalized
            .split(Regex("[\\s,;:.!?()\\[\\]{}\\-_/、。・]+"))
            .filter { it.isNotBlank() }
            .any { it == query }
    }

    fun toggleLearnedKanji(kanji: String) {
        viewModelScope.launch {
            repository.toggleLearnedKanji(kanji)
        }
    }
}