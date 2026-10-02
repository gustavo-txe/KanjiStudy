package com.app.kanjistudy.core.ui

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.app.kanjistudy.R

@Composable
fun UserMessage(message: UiText?, onDismiss: () -> Unit, onRetry: (() -> Unit)? = null) {
    if (message == null) return
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(message.resolve()) },
        confirmButton = {
            TextButton(onClick = onRetry ?: onDismiss) {
                Text(stringResource(if (onRetry == null) R.string.close else R.string.retry))
            }
        },
        dismissButton = if (onRetry != null) {
            { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } }
        } else null
    )
}
