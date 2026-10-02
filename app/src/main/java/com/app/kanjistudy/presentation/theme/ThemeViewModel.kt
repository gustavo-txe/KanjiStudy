package com.app.kanjistudy.presentation.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.R
import com.app.kanjistudy.core.ui.UiText
import com.app.kanjistudy.data.preferences.ThemePreferenceRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@HiltViewModel
class ThemeViewModel @Inject constructor(
    private val themePreferenceRepository: ThemePreferenceRepository
) : ViewModel() {

    private val _userMessage = MutableStateFlow<UiText?>(null)
    val userMessage = _userMessage.asStateFlow()

    fun clearUserMessage() {
        _userMessage.value = null
    }

    val isDarkTheme: StateFlow<Boolean> =
        themePreferenceRepository.isDarkTheme()
            .retryWhen { cause, _ ->
                if (cause !is Exception || cause is CancellationException) throw cause
                _userMessage.value = UiText(R.string.preferences_load_error)
                delay(2_000)
                true
            }
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5_000),
                initialValue = true
            )

    fun onThemeChanged(enabled: Boolean) {
        viewModelScope.launch {
            try {
                themePreferenceRepository.setDarkTheme(enabled)
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _userMessage.value = UiText(R.string.preferences_save_error)
            }
        }
    }
}
