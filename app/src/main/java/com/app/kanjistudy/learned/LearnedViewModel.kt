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
                        filteredKanjis = filterKanjis(kanjis, state.query)
                    )
                }
            }
        }
    }

    private fun filterKanjis(
        kanjis: List<KanjiData>,
        query: String
    ): List<KanjiData> {

        val lowerQuery = query.trim().lowercase()
        if (lowerQuery.isBlank()) return kanjis

        return kanjis.filter { kanji ->
            kanji.kanji.contains(lowerQuery) ||
                    kanji.kunReadings.joinToString(" ").lowercase().contains(lowerQuery) ||
                    kanji.onReadings.joinToString(" ").lowercase().contains(lowerQuery) ||
                    kanji.meanings.joinToString(" ").lowercase().contains(lowerQuery)
        }
    }

    fun toggleLearnedKanji(kanji: String) {
        viewModelScope.launch {
            repository.toggleLearnedKanji(kanji)
        }
    }
}