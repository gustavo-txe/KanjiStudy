package com.app.kanjistudy.presentation.home

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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.kanjistudy.R
import com.app.kanjistudy.core.navigation.CustomTab
import com.app.kanjistudy.core.navigation.googleAISearch
import com.app.kanjistudy.core.ui.UserMessage
import com.app.kanjistudy.data.preferences.OnboardingManager
import com.app.kanjistudy.presentation.home.components.KanjiActionCard
import com.app.kanjistudy.presentation.home.components.KanjiLoadingScreen
import com.app.kanjistudy.presentation.home.components.SearchBarKanji
import com.app.kanjistudy.presentation.onboarding.OnboardingOverlay
import kotlin.collections.forEach
import kotlinx.coroutines.launch

@Composable
fun KanjiListScreen(
    viewModel: KanjiViewModel = hiltViewModel(),
    onboardingManager: OnboardingManager
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    UserMessage(uiState.userMessage, viewModel::clearUserMessage)

    val context = LocalContext.current
    val customTab = remember { CustomTab() }
    val clipboardManager = LocalClipboardManager.current
    val coroutineScope = rememberCoroutineScope()

    val kanjisKunMap = uiState.kunReadings
    val kanjisOnMap = uiState.onReadings
    val kanjisMeanings = uiState.meanings

    val learnedKanjis by viewModel.learnedKanjis.collectAsStateWithLifecycle()

    val learnedKanjiSet = remember(learnedKanjis) {
        learnedKanjis.filter { it.isLearned }.map { it.kanji }.toSet()
    }

    var dialogType by remember { mutableStateOf<KanjiDialog?>(null) }

    val shouldShowHomeHint by onboardingManager.shouldShowHint("home")
        .collectAsStateWithLifecycle(initialValue = false)
    var homeHintDismissedInComposition by remember { mutableStateOf(false) }
    val showHomeHint = shouldShowHomeHint && !homeHintDismissedInComposition

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

    LaunchedEffect(uiState.isLoading, showHomeHint) {
        if (!uiState.isLoading && showHomeHint) {
            onboardingManager.markHintShown("home")
            homeHintDismissedInComposition = true
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
        KanjiLoadingScreen(
            uiState = uiState,
            onRetry = viewModel::retryLoading,
            modifier = Modifier
        )

        SearchBarKanji(
            queryInput = uiState.queryInput,
            onQueryChange = viewModel::onQueryChange,
            onSearchRequest = viewModel::onSearchRequested
        )
        LazyColumn {
            items(
                items = uiState.filteredKanjis,
                key = { it }) { kanji ->

                val kunReadings = kanjisKunMap[kanji] ?: listOf(stringResource(R.string.loading))
                val onReadings = kanjisOnMap[kanji] ?: listOf(stringResource(R.string.loading))
                val kanjisMeanings = kanjisMeanings[kanji] ?: listOf(stringResource(R.string.loading))
                val jlptLevel = uiState.jlptLevels[kanji]

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
                            .fillMaxSize()
                            .align(Alignment.End)
                            .clickable {
                                viewModel.addLearnedKanji(kanji, isLearned)
                            }) {
                        Text(
                            text = jlptLevel?.let { stringResource(R.string.jlpt_level, it) } ?: stringResource(R.string.jlpt_n_a),
                            fontSize = 16.sp,
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(
                                    start = 20.dp,
                                    top = 12.dp, bottom = 12.dp, end = 12.dp
                                ),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.SemiBold
                        )
                        Icon(
                            painter = if (!isLearned) painterResource(id = R.drawable.baseline_add_24)
                            else painterResource(id = R.drawable.checkicon),
                            contentDescription = stringResource(R.string.learned_status),
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
                                .clickable { viewModel.onKanjiSelected(kanji) }
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
                                    stringResource(R.string.kun_yomi),
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
                                    stringResource(R.string.on_yomi),
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
                                    stringResource(R.string.meanings),
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

            is KanjiDialog.Actions -> {
                val kanji = currentDialog.kanji

                AlertDialog(
                    onDismissRequest = { dialogType = null },
                    title = { Text(stringResource(R.string.options)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            Text(
                                text = stringResource(R.string.kanji_actions_description, kanji),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            KanjiActionCard(
                                icon = {
                                    Icon(
                                        Icons.Default.ContentCopy,
                                        contentDescription = null
                                    )
                                },
                                title = stringResource(R.string.copy_kanji_title),
                                description = stringResource(R.string.copy_kanji_description, kanji),
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(kanji))
                                    dialogType = null
                                }
                            )
                            KanjiActionCard(
                                icon = { Icon(Icons.Default.Search, contentDescription = null) },
                                title = stringResource(R.string.search_with_google_ai),
                                description = stringResource(R.string.google_search_description),
                                onClick = {
                                    customTab.openCustomTab(
                                        context,
                                        googleAISearch(kanji)
                                    )
                                    dialogType = null
                                }
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = { dialogType = null }) { Text(stringResource(R.string.close)) }
                    }
                )
            }

            is KanjiDialog.ToggleLearned -> {
                val kanji = currentDialog.kanji
                val isLearned = currentDialog.isLearned

                AlertDialog(
                    onDismissRequest = { dialogType = null },
                    title = {
                        Text(if (isLearned) stringResource(R.string.remove_kanji_title) else stringResource(R.string.add_kanji))
                    },
                    text = {
                        Text(
                            if (isLearned)
                                stringResource(R.string.remove_kanji_confirmation, kanji)
                            else
                                stringResource(R.string.add_kanji_confirmation, kanji)
                        )
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.markLearnedKanji(kanji)
                            dialogType = null
                        }) { Text(stringResource(R.string.yes)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { dialogType = null }) { Text(stringResource(R.string.no)) }
                    }
                )
            }

            null -> Unit
        }
        if (showHomeHint) {
            OnboardingOverlay(
                message = stringResource(R.string.home_hint),
                onDismiss = {
                    homeHintDismissedInComposition = true
                    coroutineScope.launch {
                        onboardingManager.markHintShown("home")
                    }
                }
            )
        }
    }
}
