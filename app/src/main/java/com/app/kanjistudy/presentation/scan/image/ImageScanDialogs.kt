package com.app.kanjistudy.presentation.scan.image

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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.kanjistudy.R
import com.app.kanjistudy.domain.model.Kanji
import com.app.kanjistudy.presentation.scan.camera.components.StatusToggleIcon

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
                text = stringResource(R.string.scan_actions_description),
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
                Text(stringResource(R.string.search))
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
                    Text(stringResource(R.string.copy))
                }
                joyoKanji?.let {
                    TextButton(onClick = { onDetails(it) }) { Text(stringResource(R.string.details)) }
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
        title = { Text(stringResource(R.string.open_google)) },
        text = { Text(stringResource(R.string.google_search_confirmation, kanji)) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.yes)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) } },
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
                    DetailColumn(title = stringResource(R.string.kun_yomi_title), items = kanji.kunReadings)
                    DetailColumn(title = stringResource(R.string.on_yomi_title), items = kanji.onReadings)
                    DetailColumn(title = stringResource(R.string.meanings_title), items = kanji.meanings)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) }
        },
        dismissButton = {
            TextButton(onClick = onSearch) { Text(stringResource(R.string.search_with_google_ai)) }
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
        title = { Text(if (isLearned) stringResource(R.string.remove_kanji_title) else stringResource(R.string.add_kanji)) },
        text = {
            Text(
                if (isLearned) {
                    stringResource(R.string.image_remove_confirmation, kanji)
                } else {
                    stringResource(R.string.image_add_confirmation, kanji)
                },
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.yes)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) } },
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
            text = jlpt?.let { stringResource(R.string.jlpt_level, it) } ?: stringResource(R.string.joyo_kanji),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        StatusToggleIcon(
            isLearned = isLearned,
            onToggle = onToggleLearned,
        )
    }
}
