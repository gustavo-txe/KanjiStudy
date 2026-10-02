package com.app.kanjistudy.presentation.learned

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.R
import com.app.kanjistudy.core.ui.UiText
import com.app.kanjistudy.core.ui.toProgressErrorMessage
import com.app.kanjistudy.data.repository.KanjiRepository
import com.app.kanjistudy.domain.search.KanjiSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@HiltViewModel
class LearnedViewModel @Inject constructor(
    private val repository: KanjiRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LearnedUiState())
    val uiState = _uiState.asStateFlow()

    private var observationJob: Job? = null

    init {
        observeLearnedKanjis()
    }

    fun observeLearnedKanjis() {
        observationJob?.cancel()
        _uiState.update { it.copy(error = null) }
        observationJob = viewModelScope.launch {
            repository.getLearnedKanjis().catch {
                _uiState.update { it.copy(error = UiText(R.string.progress_load_error)) }
            }.collect { kanjis ->
                _uiState.update { state ->
                    state.copy(
                        kanjis = kanjis,
                        filteredKanjis = KanjiSearch.filter(kanjis, state.query, state.selectedJlptLevel)
                    )
                }
            }
        }
    }

    fun onJlptLevelSelected(level: Int?) {
        _uiState.update { state ->
            state.copy(
                selectedJlptLevel = level,
                filteredKanjis = KanjiSearch.filter(state.kanjis, state.query, level)
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
    }

    fun toggleLearnedKanji(kanji: String) {
        viewModelScope.launch {
            try {
                repository.toggleLearnedKanji(kanji)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(userMessage = exception.toProgressErrorMessage()) }
            }
        }
    }
}
