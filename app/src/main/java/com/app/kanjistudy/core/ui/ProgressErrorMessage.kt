package com.app.kanjistudy.core.ui

import androidx.annotation.StringRes
import com.app.kanjistudy.R
import com.app.kanjistudy.data.repository.KanjiCatalogIncompleteException
import com.app.kanjistudy.data.repository.ProgressRestorationPendingException

fun Throwable.toProgressErrorMessage(@StringRes fallback: Int = R.string.progress_save_error): UiText =
    UiText(
        when (this) {
            is KanjiCatalogIncompleteException -> R.string.progress_download_pending
            is ProgressRestorationPendingException -> R.string.progress_restore_pending
            else -> fallback
        }
    )
