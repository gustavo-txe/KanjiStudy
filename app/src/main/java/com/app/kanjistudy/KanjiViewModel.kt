package com.app.kanjistudy

import android.util.Log
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class KanjiViewModel @Inject constructor(private val repository: KanjiRepository) : ViewModel() {

    private val _joyoKanjis = mutableStateOf<List<String>>(emptyList())
    val joyoKanjis: State<List<String>> = _joyoKanjis

    private val _kanjisKunMap = mutableStateOf<Map<String, List<String>>>(emptyMap())
    val kanjisKunMap: State<Map<String, List<String>>> = _kanjisKunMap

    private val _kanjisOnMap = mutableStateOf<Map<String, List<String>>>(emptyMap())
    val kanjisOnMap: State<Map<String, List<String>>> = _kanjisOnMap

    private val _kanjisMeanings = mutableStateOf<Map<String, List<String>>>(emptyMap())
    val kanjisMeanings: State<Map<String, List<String>>> = _kanjisMeanings

    init {

        getJoyo()

    }

    fun getJoyo() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val joyo = repository.getJoyoKanjis()
                withContext(Dispatchers.Main) {
                    _joyoKanjis.value = joyo
                }
            } catch (e: Exception) {
                Log.e("KanjiViewModel", "Erro ao buscar kanjis", e)
            }
        }
    }


    fun getKanjisKun(kanji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val kunReadings = repository.getJoyoKunReading(kanji)
                withContext(Dispatchers.Main) {
                    _kanjisKunMap.value += (kanji to kunReadings)
                }
            } catch (e: Exception) {
                Log.e("KanjiViewModel", "Erro ao buscar kanjis", e)
            }
        }
    }

    fun getKanjisOn(kanji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val onReadings = repository.getJoyoOnReading(kanji)
                withContext(Dispatchers.Main) {
                    _kanjisOnMap.value += (kanji to onReadings)
                }
            } catch (e: Exception) {
                Log.e("KanjiViewModel", "Erro ao buscar kanjis", e)
            }
        }
    }

    fun getJoyoKanjiMeanings(kanji: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val meanings = repository.getJoyoKanjiMeanings(kanji)
                withContext(Dispatchers.Main) {
                    _kanjisMeanings.value += (kanji to meanings)
                }
            } catch (e: Exception) {
                Log.e("KanjiViewModel", "Erro ao buscar kanjis", e)
            }
        }
    }


}