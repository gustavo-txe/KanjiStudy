package com.app.kanjistudy.learned

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.kanjistudy.data.model.KanjiData
import com.app.kanjistudy.onboarding.OnboardingManager
import com.app.kanjistudy.onboarding.OnboardingOverlay

private const val TOTAL_JOYO_KANJI = 2136

@Composable
fun LearnedScreen(
    viewModel: LearnedViewModel = hiltViewModel(),
    onboardingManager: OnboardingManager
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    var selectedKanji by remember { mutableStateOf<KanjiData?>(null) }

    var showLearnedHint by remember { mutableStateOf(onboardingManager.shouldShowHint("learned")) }

    Box(modifier = Modifier.fillMaxSize()) {
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

        val learnedCount = uiState.kanjis.size
        val progress = (learnedCount.toFloat() / TOTAL_JOYO_KANJI).coerceIn(0f, 1f)

        Column(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                ) {
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier.fillMaxWidth().height(15.dp).padding(0.dp, 5.dp,
                        0.dp, 0.dp),
                    color = Color(0xFF048006),

                )
                Text(
                    text = "$learnedCount/$TOTAL_JOYO_KANJI Jōyō kanji learned",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(top = 8.dp).align(Alignment.CenterHorizontally)
                )
            }

            if (uiState.filteredKanjis.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "You haven't marked any learned kanji yet.",
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(uiState.filteredKanjis) { kanji ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(140.dp)
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
                                    fontSize = 50.sp
                                )
                            }
                        }
                    }
                }
            }
        }

        if (showLearnedHint) {
            OnboardingOverlay(
                message = "Learned Jōyō kanji\n\nThis screen shows all Jōyō kanji you marked as learned so you can track your progress.",
                onDismiss = {
                    onboardingManager.markHintShown("learned")
                    showLearnedHint = false
                }
            )
        }
    }
}
