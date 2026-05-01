package com.app.kanjistudy.learned

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.kanjistudy.data.model.KanjiData
import com.app.kanjistudy.onboarding.OnboardingManager

@Composable
fun LearnedScreen(
    viewModel: LearnedViewModel = hiltViewModel(),
    onboardingManager: OnboardingManager
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedKanji by remember { mutableStateOf<KanjiData?>(null) }

    var showLearnedHint by remember { mutableStateOf(onboardingManager.shouldShowHint("learned")) }

    if (showLearnedHint) {
        AlertDialog(
            onDismissRequest = {
                onboardingManager.markHintShown("learned")
                showLearnedHint = false
            },
            title = { Text("Tip: Learned Jōyō kanji") },
            text = { Text("This screen shows all Jōyō kanji you marked as learned so you can track your progress.") },
            confirmButton = {
                TextButton(onClick = {
                    onboardingManager.markHintShown("learned")
                    showLearnedHint = false
                }) { Text("Got it") }
            }
        )
    }

    selectedKanji?.let { kanji ->
        AlertDialog(
            onDismissRequest = { selectedKanji = null },
            title = { Text("Remove Kanji?") },
            text = {
                Text("Would you like to remove the kanji ${kanji.kanji} as learned?")
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.toggleLearnedKanji(kanji.kanji)
                    selectedKanji = null
                }) {
                    Text("Yes")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedKanji = null }) {
                    Text("No")
                }
            }
        )
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(3),
        contentPadding = PaddingValues(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        items(uiState.filteredKanjis) { kanji ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(150.dp)
                    .clickable {
                        selectedKanji = kanji
                    },
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = kanji.kanji,
                        fontSize = 60.sp
                    )
                }
            }
        }
    }
}
