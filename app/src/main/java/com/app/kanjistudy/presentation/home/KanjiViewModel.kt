package com.app.kanjistudy.presentation.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.R
import com.app.kanjistudy.core.ui.UiText
import com.app.kanjistudy.core.ui.toProgressErrorMessage
import com.app.kanjistudy.data.repository.KanjiRepository
import com.app.kanjistudy.di.DefaultDispatcher
import com.app.kanjistudy.domain.model.Kanji
import com.app.kanjistudy.domain.search.KanjiSearch
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@HiltViewModel
class KanjiViewModel @Inject constructor(
    private val repository: KanjiRepository,
    @DefaultDispatcher private val filterDispatcher: CoroutineDispatcher
) : ViewModel() {

    private var catalog: List<Kanji> = emptyList()

    private var filterJob: Job? = null
    private var loadJob: Job? = null

    private val _uiState = MutableStateFlow(KanjiUiState())
    val uiState: StateFlow<KanjiUiState> = _uiState.asStateFlow()

    private val _uiEvent = MutableSharedFlow<KanjiUiEvent>()
    val uiEvent: SharedFlow<KanjiUiEvent?> = _uiEvent.asSharedFlow()

    val learnedKanjis: StateFlow<List<Kanji>> = repository.getLearnedKanjis()
        .retryWhen { cause, _ ->
            if (cause !is Exception || cause is CancellationException) throw cause
            _uiState.update { it.copy(userMessage = UiText(R.string.progress_load_error)) }
            delay(2_000)
            true
        }
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
            try {
                repository.toggleLearnedKanji(kanji)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _uiState.update { it.copy(userMessage = exception.toProgressErrorMessage()) }
            }
        }
    }

    fun retryLoading() = loadKanjis()

    private fun loadKanjis() {
        if (loadJob?.isActive == true) return
        loadJob = viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, loadingProgress = 0f) }

            try {
                val localData = repository.ensureAllKanjisLoaded(
                    onProgress = { progress ->
                        _uiState.update { it.copy(loadingProgress = progress) }
                    }
                )
                catalog = localData
                updateMaps(localData)

                _uiState.update {
                    val joyoKanjis = localData.map { kanji -> kanji.kanji }
                    it.copy(
                        joyoKanjis = joyoKanjis,
                        isLoading = false,
                        isCatalogReady = true,
                        loadingProgress = 1f,
                        filteredKanjis = applyFilters(it.copy(joyoKanjis = joyoKanjis))
                    )
                }

            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = UiText(R.string.catalog_load_error)
                    )
                }
            }

        }
    }

    private fun updateMaps(data: List<Kanji>) {
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
        _uiState.update { it.copy(queryInput = query) }
    }

    fun onSearchRequested() {
        _uiState.update { it.copy(submittedQuery = it.queryInput) }
        updateFilters()
    }

    fun onJlptLevelSelected(level: Int?) {
        _uiState.update { it.copy(selectedJlptLevel = level) }
        updateFilters()
    }

    private fun updateFilters() {
        filterJob?.cancel()
        val state = _uiState.value
        val entries = catalog
        filterJob = viewModelScope.launch {
            val filteredKanjis = withContext(filterDispatcher) {
                KanjiSearch.filter(entries, state.submittedQuery, state.selectedJlptLevel).map { it.kanji }
            }
            _uiState.update { it.copy(filteredKanjis = filteredKanjis) }
        }
    }

    private fun applyFilters(state: KanjiUiState): List<String> =
        KanjiSearch.filter(catalog, state.submittedQuery, state.selectedJlptLevel).map { it.kanji }

    fun clearUserMessage() {
        _uiState.update { it.copy(userMessage = null) }
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

    fun onKanjiSelected(kanji: String) {
        viewModelScope.launch {
            _uiEvent.emit(
                KanjiUiEvent.ShowDialog(
                    KanjiDialog.Actions(kanji)
                )
            )
        }
    }
}
