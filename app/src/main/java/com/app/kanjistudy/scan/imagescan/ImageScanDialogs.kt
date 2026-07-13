package com.app.kanjistudy.scan.imagescan

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.kanjistudy.domain.model.Kanji
import com.app.kanjistudy.scan.camerascan.components.StatusToggleIcon

@Composable
internal fun KanjiOptionsDialog(
    kanji: Char,
    joyoKanji: Kanji?,
    isLearned: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onDetails: (Kanji) -> Unit,
    onSearch: () -> Unit,
    onToggleLearned: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                joyoKanji?.let {
                    DialogTitleMetadata(
                        jlpt = it.jlpt,
                        isLearned = isLearned,
                        onToggleLearned = onToggleLearned,
                    )
                }
                Text(
                    text = kanji.toString(),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 96.sp,
                    lineHeight = 98.sp,
                )
            }
        },
        text = {
            Text(
                text = "Choose an action for this kanji:",
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        confirmButton = {
            TextButton(onClick = onSearch) {
                DialogActionIcon(
                    type = DialogActionIconType.Search,
                    contentDescription = null,
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text("Search")
            }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onCopy) {
                    DialogActionIcon(
                        type = DialogActionIconType.Copy,
                        contentDescription = null,
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    Text("Copy")
                }
                joyoKanji?.let {
                    TextButton(onClick = { onDetails(it) }) { Text("Details") }
                }
            }
        },
    )
}

@Composable
internal fun GoogleSearchDialog(
    kanji: Char,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open Google?") },
        text = { Text("Would you like to search for $kanji with Google AI?") },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Yes") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("No") } },
    )
}

@Composable
internal fun KanjiDetailsDialog(
    kanji: Kanji,
    isLearned: Boolean,
    onDismiss: () -> Unit,
    onSearch: () -> Unit,
    onToggleLearned: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            DialogTitleMetadata(
                jlpt = kanji.jlpt,
                isLearned = isLearned,
                onToggleLearned = onToggleLearned,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(text = kanji.kanji, fontSize = 92.sp, lineHeight = 94.sp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    DetailColumn(title = "Kun'yomi", items = kanji.kunReadings)
                    DetailColumn(title = "On'yomi", items = kanji.onReadings)
                    DetailColumn(title = "Meanings", items = kanji.meanings)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
        dismissButton = {
            TextButton(onClick = onSearch) { Text("Search with Google AI") }
        },
    )
}

@Composable
internal fun ToggleLearnedDialog(
    kanji: Char,
    isLearned: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isLearned) "Remove Kanji?" else "Add Kanji?") },
        text = {
            Text(
                if (isLearned) {
                    "Would you like to remove $kanji from learned kanji?"
                } else {
                    "Would you like to add $kanji to learned kanji?"
                },
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text("Yes") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("No") } },
    )
}

@Composable
private fun DialogTitleMetadata(
    jlpt: Int?,
    isLearned: Boolean,
    onToggleLearned: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = jlpt?.let { "JLPT $it" } ?: "Joyo kanji",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusToggleIcon(
            isLearned = isLearned,
            onToggle = onToggleLearned,
        )
    }
}