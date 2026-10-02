package com.app.kanjistudy.presentation.scan.camera.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.app.kanjistudy.R
import com.app.kanjistudy.domain.model.Kanji
import com.app.kanjistudy.presentation.scan.camera.CameraScanUiState

@Composable
fun CameraScanDialogs(
    selectedKanji: Char?,
    googleSearchKanji: String?,
    detailsKanji: Kanji?,
    learnedToggleKanji: Char?,
    uiState: CameraScanUiState,
    onDismissSelected: () -> Unit,
    onDismissGoogleSearch: () -> Unit,
    onConfirmGoogleSearch: (String) -> Unit,
    onDismissDetails: () -> Unit,
    onDismissLearnedToggle: () -> Unit,
    onCopyKanji: (Char) -> Unit,
    onOpenGoogleConfirmation: (String) -> Unit,
    onOpenDetails: (Kanji) -> Unit,
    onLearnedToggleRequest: (Char) -> Unit,
    onConfirmLearnedToggle: (Char) -> Unit,
) {
    selectedKanji?.let { kanji ->
        SelectedKanjiDialog(
            kanji = kanji,
            kanjiData = uiState.recognizedJoyoKanjis[kanji],
            isLearned = uiState.learnedKanjis.contains(kanji),
            onDismiss = onDismissSelected,
            onCopy = {
                onCopyKanji(kanji)
                onDismissSelected()
            },
            onOpenGoogle = {
                onOpenGoogleConfirmation(kanji.toString())
                onDismissSelected()
            },
            onOpenDetails = { data ->
                onOpenDetails(data)
                onDismissSelected()
            },
            onToggleLearned = { onLearnedToggleRequest(kanji) },
        )
    }

    googleSearchKanji?.let { kanji ->
        GoogleSearchDialog(
            kanji = kanji,
            onDismiss = onDismissGoogleSearch,
            onConfirm = { onConfirmGoogleSearch(kanji) },
        )
    }

    detailsKanji?.let { kanjiData ->
        KanjiDetailsDialog(
            kanjiData = kanjiData,
            isLearned = uiState.learnedKanjis.contains(kanjiData.kanji.first()),
            onDismiss = onDismissDetails,
            onOpenGoogle = {
                onOpenGoogleConfirmation(kanjiData.kanji)
                onDismissDetails()
            },
            onToggleLearned = { onLearnedToggleRequest(kanjiData.kanji.first()) },
        )
    }

    learnedToggleKanji?.let { kanji ->
        ToggleLearnedDialog(
            kanji = kanji,
            isLearned = uiState.learnedKanjis.contains(kanji),
            onDismiss = onDismissLearnedToggle,
            onConfirm = {
                onConfirmLearnedToggle(kanji)
                onDismissLearnedToggle()
            },
        )
    }
}

@Composable
private fun SelectedKanjiDialog(
    kanji: Char,
    kanjiData: Kanji?,
    isLearned: Boolean,
    onDismiss: () -> Unit,
    onCopy: () -> Unit,
    onOpenGoogle: () -> Unit,
    onOpenDetails: (Kanji) -> Unit,
    onToggleLearned: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Column(modifier = Modifier.fillMaxWidth()) {
                kanjiData?.let { data ->
                    KanjiDialogHeader(
                        jlpt = data.jlpt,
                        isLearned = isLearned,
                        onToggleLearned = onToggleLearned,
                    )
                }
                Text(
                    text = kanji.toString(),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    fontSize = 100.sp,
                    lineHeight = 104.sp,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenGoogle) { Text(stringResource(R.string.search_with_google_ai)) }
        },
        dismissButton = {
            Row {
                TextButton(onClick = onCopy) { Text(stringResource(R.string.copy)) }
                kanjiData?.let { data ->
                    TextButton(onClick = { onOpenDetails(data) }) { Text(stringResource(R.string.details)) }
                }
            }
        },
    )
}

@Composable
private fun GoogleSearchDialog(
    kanji: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
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
private fun KanjiDetailsDialog(
    kanjiData: Kanji,
    isLearned: Boolean,
    onDismiss: () -> Unit,
    onOpenGoogle: () -> Unit,
    onToggleLearned: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            KanjiDialogHeader(
                jlpt = kanjiData.jlpt,
                isLearned = isLearned,
                onToggleLearned = onToggleLearned,
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = kanjiData.kanji,
                    fontSize = 96.sp,
                    lineHeight = 98.sp,
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceAround,
                ) {
                    DetailColumn(title = stringResource(R.string.kun_yomi_title), items = kanjiData.kunReadings)
                    DetailColumn(title = stringResource(R.string.on_yomi_title), items = kanjiData.onReadings)
                    DetailColumn(title = stringResource(R.string.meanings_title), items = kanjiData.meanings)
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.close)) } },
        dismissButton = { TextButton(onClick = onOpenGoogle) { Text(stringResource(R.string.search_with_google_ai)) } },
    )
}

@Composable
private fun ToggleLearnedDialog(
    kanji: Char,
    isLearned: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isLearned) stringResource(R.string.remove_kanji_title) else stringResource(R.string.add_kanji)) },
        text = {
            Text(
                if (isLearned) {
                    stringResource(R.string.camera_remove_confirmation, kanji)
                } else {
                    stringResource(R.string.camera_add_confirmation, kanji)
                },
            )
        },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.yes)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.no)) } },
    )
}

@Composable
private fun KanjiDialogHeader(
    jlpt: Int?,
    isLearned: Boolean,
    onToggleLearned: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (jlpt != null) {
            Text(
                text = stringResource(R.string.jlpt_level, jlpt),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        StatusToggleIcon(
            isLearned = isLearned,
            onToggle = onToggleLearned,
        )
    }
}

@Composable
private fun DetailColumn(title: String, items: List<String>) {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
        )
        items.forEach { item ->
            Text(
                text = item,
                fontSize = 13.sp,
            )
        }
    }
}
