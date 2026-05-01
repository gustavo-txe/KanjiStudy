package com.app.kanjistudy.home.kanjis

import androidx.compose.animation.animateColor
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import com.app.kanjistudy.onboarding.OnboardingManager
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.kanjistudy.usecase.CustomTab
import com.app.kanjistudy.R
import com.app.kanjistudy.home.kanjis.components.KanjiLoadingScreen
import com.app.kanjistudy.home.kanjis.components.SearchBarKanji
import kotlin.collections.forEach

@Composable
fun KanjiListScreen(
    viewModel: KanjiViewModel = hiltViewModel(),
    onboardingManager: OnboardingManager
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val customTab = remember { CustomTab() }

    val kanjisKunMap = uiState.kunReadings
    val kanjisOnMap = uiState.onReadings
    val kanjisMeanings = uiState.meanings

    val learnedKanjis by viewModel.learnedKanjis.collectAsStateWithLifecycle()

    val learnedKanjiSet = remember(learnedKanjis) {
        learnedKanjis.filter { it.isLearned }.map { it.kanji }.toSet()
    }

    var dialogType by remember { mutableStateOf<KanjiDialog?>(null) }

    var showHomeHint by remember { mutableStateOf(onboardingManager.shouldShowHint("home")) }

    if (showHomeHint) {
        AlertDialog(
            onDismissRequest = {
                onboardingManager.markHintShown("home")
                showHomeHint = false
            },
            title = { Text("Tip: Home Screen") },
            text = { Text("Wait for the download progress bar to finish. Then you can search kanji and mark or unmark them as learned.") },
            confirmButton = {
                TextButton(onClick = {
                    onboardingManager.markHintShown("home")
                    showHomeHint = false
                }) { Text("Got it") }
            }
        )
    }


    LaunchedEffect(Unit) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is KanjiUiEvent.ShowDialog -> {
                    dialogType = event.dialog
                }
                else -> Unit
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "infiniteColor")

    val animatedColor by infiniteTransition.animateColor(
        initialValue = Color(0xFF28D0A1),
        targetValue = Color(0xFF156B18),
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "colorCycle"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        KanjiLoadingScreen(viewModel)

        SearchBarKanji(onSearch = viewModel::onQueryChange)

        LazyColumn {
            items(items= uiState.filteredKanjis,
                key = { it }) { kanji ->

                val kunReadings = kanjisKunMap[kanji] ?: listOf("loading...")
                val onReadings = kanjisOnMap[kanji] ?: listOf("loading...")
                val kanjisMeanings = kanjisMeanings[kanji] ?: listOf("loading...")

                val isLearned = learnedKanjiSet.contains(kanji)

                Card(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 16.dp, horizontal = 16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 7.dp),
                    colors = CardDefaults.cardColors(
                        containerColor =
                            MaterialTheme.colorScheme.surface
                    )
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .align(Alignment.End)
                            .clickable {
                                viewModel.addLearnedKanji(kanji, isLearned)
                            }) {
                        Icon(
                            painter = if (!isLearned) painterResource(id = R.drawable.baseline_add_24)
                            else painterResource(id = R.drawable.checkicon),
                            contentDescription = "Ícone",
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(40.dp),
                            tint = if (isLearned) animatedColor else Color.Gray,
                        )
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center

                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.openGoogleSearch(kanji)
                                }
                        ) {
                            Text(
                                text = kanji,
                                fontSize = 150.sp,
                                modifier = Modifier.align(Alignment.Center),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Start

                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {

                                Text(
                                    "Kun'yomi:",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                kunReadings.forEach {
                                    Text(
                                        text = it, fontSize = 15.sp,
                                    )
                                }
                            }
                            Column(modifier = Modifier.padding(16.dp)) {

                                Text(
                                    "On'yomi:",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                onReadings.forEach {
                                    Text(
                                        text = it, fontSize = 15.sp,
                                    )
                                }
                            }
                            Column(modifier = Modifier.padding(16.dp)) {

                                Text(
                                    "Meanings:",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                kanjisMeanings.forEach {
                                    Text(
                                        text = it, fontSize = 15.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        when (val currentDialog = dialogType) {

            is KanjiDialog.Google -> {
                val kanji = currentDialog.kanji

                AlertDialog(
                    onDismissRequest = { dialogType = null },
                    title = { Text("Open Google?") },
                    text = { Text("Would you like to search for $kanji on Google?") },
                    confirmButton = {
                        TextButton(onClick = {
                            customTab.openCustomTab(
                                context,
                                "https://www.google.com/search?q=kanji+$kanji"
                            )
                            dialogType = null
                        }) { Text("Yes") }
                    },
                    dismissButton = {
                        TextButton(onClick = { dialogType = null }) { Text("No") }
                    }
                )
            }

            is KanjiDialog.ToggleLearned -> {
                val kanji = currentDialog.kanji
                val isLearned = currentDialog.isLearned

                AlertDialog(
                    onDismissRequest = { dialogType = null },
                    title = {
                        Text(if (isLearned) "Remove learned kanji?" else "Add kanji?")
                    },
                    text = {
                        Text(
                            if (isLearned)
                                "Would you like to remove $kanji from learned?"
                            else
                                "Would you like to add $kanji as learned?"
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.markLearnedKanji(kanji)
                            dialogType = null
                        }) { Text("Yes") }
                    },
                    dismissButton = {
                        TextButton(onClick = { dialogType = null }) { Text("No") }
                    }
                )
            }

            null -> Unit
        }
    }
}


