package com.app.kanjistudy.core.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource

data class UiText(@StringRes val resource: Int, val arguments: List<Any> = emptyList()) {
    fun resolve(context: Context): String = context.getString(resource, *arguments.toTypedArray())

    @Composable
    fun resolve(): String = stringResource(resource, *arguments.toTypedArray())
}
