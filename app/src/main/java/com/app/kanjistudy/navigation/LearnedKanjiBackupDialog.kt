package com.app.kanjistudy.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

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
        title = { Text("Learned Kanji Backup") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = "Import or export your learned kanji as a JSON file. Imported kanji are merged with your current progress.",
                    style = MaterialTheme.typography.bodyMedium
                )

                if (isKanjiDownloading) {
                    Text(
                        text = KANJI_DOWNLOAD_IN_PROGRESS_MESSAGE,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }

                TransferOptionCard(
                    icon = { Icon(Icons.Default.FileDownload, contentDescription = null) },
                    title = "Export JSON",
                    description = "Save a backup of all kanji marked as learned on this device.",
                    enabled = !isBusy && !isKanjiDownloading,
                    onClick = onExportClick
                )
                TransferOptionCard(
                    icon = { Icon(Icons.Default.FileUpload, contentDescription = null) },
                    title = "Import JSON",
                    description = "Choose a backup file and add its kanji to your learned list.",
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
                Text("Close")
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
            text = busyMessage ?: "Working…",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}