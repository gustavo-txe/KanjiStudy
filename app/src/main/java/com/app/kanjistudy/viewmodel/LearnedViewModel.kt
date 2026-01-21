package com.app.kanjistudy.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.data.model.KanjiData
import com.app.kanjistudy.data.repository.KanjiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LearnedViewModel @Inject constructor(private val repository: KanjiRepository) : ViewModel() {

    val learnedKanjis: StateFlow<List<KanjiData>> = repository.getLearnedKanjis()
                .stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5000),
                    emptyList()
                )

    fun markLearnedKanji(kanji: String){

        viewModelScope.launch {
            val allKanjis = repository.getAllLocalKanjis()
            val kanjiData = allKanjis.find { it.kanji == kanji } ?: return@launch

            if (kanjiData.isLearned) {
                repository.uncheckLearnedKanji(kanji)
            } else {
                repository.markAsLearned(kanji)
            }

        }
    }

}
