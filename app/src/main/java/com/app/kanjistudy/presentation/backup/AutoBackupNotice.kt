package com.app.kanjistudy.presentation.backup

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.kanjistudy.R

@Composable
fun AutoBackupNotice(viewModel: AutoBackupViewModel = hiltViewModel()) {
    val pending by viewModel.isPending.collectAsStateWithLifecycle()
    val retrying by viewModel.isRetrying.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    var dismissed by remember(pending) { mutableStateOf(false) }
    if (!pending || dismissed) return
    AlertDialog(
        onDismissRequest = { if (!retrying) dismissed = true },
        text = { Text(error?.resolve() ?: stringResource(R.string.auto_backup_pending)) },
        confirmButton = {
            TextButton(onClick = viewModel::retry, enabled = !retrying) {
                Text(stringResource(R.string.retry))
            }
        },
        dismissButton = {
            TextButton(onClick = { dismissed = true }, enabled = !retrying) {
                Text(stringResource(R.string.close))
            }
        }
    )
}
