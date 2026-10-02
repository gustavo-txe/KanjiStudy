package com.app.kanjistudy.presentation.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.ImportExport
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.app.kanjistudy.R

val LEARNED_KANJI_IMPORT_MIME_TYPES = arrayOf("application/json", "text/json", "text/*", "*/*")

@Composable
fun LearnedKanjiBackupDialog(
    isBusy: Boolean,
    isKanjiDownloading: Boolean,
    busyMessage: String?,
    onDismiss: () -> Unit,
    onImportClick: () -> Unit,
    onExportClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Default.ImportExport, contentDescription = null) },
        title = { Text(stringResource(R.string.learned_kanji_backup)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.backup_description),
                    style = MaterialTheme.typography.bodyMedium
                )

                if (isKanjiDownloading) {
                    Text(
                        text = stringResource(KANJI_DOWNLOAD_IN_PROGRESS_MESSAGE),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                TransferOptionCard(
                    icon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                    title = stringResource(R.string.export_json),
                    description = stringResource(R.string.export_description),
                    enabled = !isBusy && !isKanjiDownloading,
                    onClick = onExportClick
                )
                TransferOptionCard(
                    icon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                    title = stringResource(R.string.import_json),
                    description = stringResource(R.string.import_description),
                    enabled = !isBusy && !isKanjiDownloading,
                    onClick = onImportClick
                )

                if (isBusy) {
                    BackupProgressIndicator(busyMessage = busyMessage)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss, enabled = !isBusy) {
                Text(stringResource(R.string.close))
            }
        }
    )
}

@Composable
private fun TransferOptionCard(
    icon: @Composable () -> Unit,
    title: String,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            icon()
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun BackupProgressIndicator(busyMessage: String?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
        Text(
            text = busyMessage ?: stringResource(R.string.working),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
