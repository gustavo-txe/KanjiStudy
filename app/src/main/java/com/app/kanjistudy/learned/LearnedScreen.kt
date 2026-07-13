package com.app.kanjistudy.learned

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.kanjistudy.domain.model.Kanji
import com.app.kanjistudy.onboarding.OnboardingManager
import com.app.kanjistudy.onboarding.OnboardingOverlay
import com.app.kanjistudy.scan.usecase.googleAISearch
import com.app.kanjistudy.usecase.CustomTab
import kotlinx.coroutines.launch

private const val TOTAL_JOYO_KANJI = 2136

@Composable
fun LearnedScreen(
    viewModel: LearnedViewModel = hiltViewModel(),
    onboardingManager: OnboardingManager
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current
    val customTab = remember { CustomTab() }
    val coroutineScope = rememberCoroutineScope()

    var showRemoveDialog by remember { mutableStateOf(false) }

    var selectedKanji by remember { mutableStateOf<Kanji?>(null) }

    val shouldShowLearnedHint by onboardingManager.shouldShowHint("learned")
        .collectAsStateWithLifecycle(initialValue = false)
    var learnedHintDismissedInComposition by remember { mutableStateOf(false) }
    val showLearnedHint = shouldShowLearnedHint && !learnedHintDismissedInComposition

    Box(modifier = Modifier.fillMaxSize()) {
        selectedKanji?.let { kanji ->
            AlertDialog(
                onDismissRequest = {
                    selectedKanji = null
                    showRemoveDialog = false
                },
                title = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Start
                    ) {
                        IconButton(onClick = {
                            clipboardManager.setText(AnnotatedString(kanji.kanji))
                            Toast.makeText(
                                context,
                                "Kanji copied!",
                                Toast.LENGTH_SHORT
                            ).show()
                        }) {
                            Icon(
                                modifier = Modifier
                                    .height(20.dp)
                                    .width(20.dp),
                                imageVector = Icons.Filled.ContentCopy,
                                contentDescription = "Copy kanji",
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        kanji.jlpt?.let { jlpt ->
                            Text(
                                text = "JLPT $jlpt",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier
                                    .align(Alignment.CenterVertically)
                                    .padding(end = 4.dp),
                            )
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        IconButton(onClick = { showRemoveDialog = true }) {
                            Icon(
                                modifier = Modifier
                                    .height(27.dp)
                                    .width(27.dp),
                                imageVector = Icons.Filled.DeleteOutline,
                                contentDescription = "Remove Kanji",
                            )
                        }
                    }
                },
                text = {
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(220)) +
                                scaleIn(animationSpec = tween(220), initialScale = 0.92f)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = kanji.kanji,
                                fontSize = 96.sp,
                                lineHeight = 98.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 8.dp),
                                horizontalArrangement = Arrangement.SpaceAround
                            ) {
                                DetailColumn(title = "Kun'yomi:", items = kanji.kunReadings)
                                DetailColumn(title = "On'yomi:", items = kanji.onReadings)
                                DetailColumn(title = "Meanings:", items = kanji.meanings)
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { selectedKanji = null }) {
                        Text("Close")
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        customTab.openCustomTab(
                            context, googleAISearch(kanji.kanji)
                        )
                        showRemoveDialog = false
                        selectedKanji = null
                    }) {
                        Text("Search with Google AI")
                    }
                }
            )
        }

        if (showRemoveDialog && selectedKanji != null) {
            AlertDialog(
                onDismissRequest = { showRemoveDialog = false },
                title = { Text("Remove Kanji?") },
                text = {
                    Text("Would you like to remove the kanji ${selectedKanji?.kanji} as learned?")
                },
                confirmButton = {
                    TextButton(onClick = {
                        selectedKanji?.let { viewModel.toggleLearnedKanji(it.kanji) }
                        showRemoveDialog = false
                        selectedKanji = null
                    }) {
                        Text("Yes")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRemoveDialog = false }) {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(15.dp)
                        .padding(
                            0.dp, 6.dp,
                            0.dp, 0.dp
                        ),
                    color = Color(0xFF048006),

                    )
                Text(
                    text = "$learnedCount/$TOTAL_JOYO_KANJI Jōyō kanji learned",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .align(Alignment.CenterHorizontally)
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
                    learnedHintDismissedInComposition = true
                    coroutineScope.launch {
                        onboardingManager.markHintShown("learned")
                    }
                }
            )
        }
    }
}

@Composable
private fun DetailColumn(
    title: String,
    items: List<String>
) {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        items.forEach {
            Text(text = it, fontSize = 13.sp)
        }
    }
}
