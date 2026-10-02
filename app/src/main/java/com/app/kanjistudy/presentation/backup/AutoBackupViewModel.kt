package com.app.kanjistudy.presentation.backup

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.kanjistudy.R
import com.app.kanjistudy.core.ui.UiText
import com.app.kanjistudy.core.ui.toProgressErrorMessage
import com.app.kanjistudy.data.repository.KanjiRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

@HiltViewModel
class AutoBackupViewModel @Inject constructor(private val repository: KanjiRepository) : ViewModel() {
    val isPending = repository.isAutoBackupPending
    private val _error = MutableStateFlow<UiText?>(null)
    val error = _error.asStateFlow()
    private val _isRetrying = MutableStateFlow(false)
    val isRetrying = _isRetrying.asStateFlow()

    fun retry() {
        if (_isRetrying.value) return
        _isRetrying.value = true
        _error.value = null
        viewModelScope.launch {
            try {
                repository.retryAutoBackup()
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: Exception) {
                _error.value = exception.toProgressErrorMessage(R.string.auto_backup_pending)
            } finally {
                _isRetrying.value = false
            }
        }
    }
}
