package com.app.kanjistudy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.KanjiUiState
import com.app.kanjistudy.data.model.KanjiData
import com.app.kanjistudy.data.repository.KanjiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class KanjiViewModel @Inject constructor(private val repository: KanjiRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(KanjiUiState())
    val uiState: StateFlow<KanjiUiState> = _uiState.asStateFlow()

    init {
        fetchAndStoreAllKanjis()
    }

     fun markLearnedKanji(kanji: String){
        viewModelScope.launch(Dispatchers.IO) {
            val allKanjis = repository.getAllLocalKanjis()

            when(allKanjis.find { it.kanji == kanji }!!.isLearned){
                true -> repository.uncheckLearnedKanji(kanji)
                false -> repository.markAsLearned(kanji)
            }
        }

    }

    fun fetchAndStoreAllKanjis() {
        viewModelScope.launch(Dispatchers.IO) {
            val existingCount = repository.countKanjis()

            if (existingCount >= 2140) {
                val localData = repository.getAllLocalKanjis()
                updateLocalMaps(localData)

                _uiState.value = _uiState.value.copy(
                    joyoKanjis = localData.map { it.kanji })

                withContext(Dispatchers.Main) {

                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        loadingProgress = 1f
                    )
                }
                return@launch
            }

            val joyoKanjis = repository.getJoyoKanjis()
            _uiState.value = _uiState.value.copy(
                joyoKanjis = joyoKanjis
            )

            val total = joyoKanjis.size
            var processed = 0

            joyoKanjis.chunked(40).forEach { chunk ->
                val kanjiDataList = chunk
                    .map { kanji ->
                        async {
                            try {
                                repository.getReadingMeaning(kanji)
                            } catch (e: Exception) {
                                null
                            }
                        }
                    }.awaitAll().filterNotNull()

                repository.insertAllKanjis(kanjiDataList)

                val localData = repository.getAllLocalKanjis()
                updateLocalMaps(localData)

                processed += chunk.size
                _uiState.value = _uiState.value.copy(
                    loadingProgress = processed.toFloat() / total
                )

                withContext(Dispatchers.Main) {
                    if (localData.size == 2140) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            loadingProgress = 1f
                        )
                    }
                }
            }
        }

    }

    private fun updateLocalMaps(localData: List<KanjiData>) {
        val kunMap = localData.associate { it.kanji to it.kunReadings }
        val onMap = localData.associate { it.kanji to it.onReadings }
        val meaningsMap = localData.associate { it.kanji to it.meanings }

        _uiState.value = _uiState.value.copy(
            kunReadings = kunMap.toMap(),
            onReadings = onMap.toMap(),
            meanings = meaningsMap.toMap()
        )
    }




}