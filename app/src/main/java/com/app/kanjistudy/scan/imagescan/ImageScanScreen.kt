package com.app.kanjistudy.scan.imagescan

import android.app.Activity
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.kanjistudy.domain.model.Kanji
import com.app.kanjistudy.help.HelpActivity
import com.app.kanjistudy.home.kanjis.components.HomeTopBar
import com.app.kanjistudy.onboarding.OnboardingManager
import com.app.kanjistudy.onboarding.OnboardingOverlay
import com.app.kanjistudy.scan.usecase.googleAISearch
import com.app.kanjistudy.usecase.CustomTab
import kotlinx.coroutines.launch

private const val IMAGE_SCAN_HINT_KEY = "image_scan"

@Composable
fun ImageScanScreen(
    onboardingManager: OnboardingManager,
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    viewModel: ImageScanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val customTab = remember { CustomTab() }
    val coroutineScope = rememberCoroutineScope()
    val imageSize = (configuration.screenWidthDp.dp - 40.dp).coerceIn(220.dp, 520.dp)
    val latestOnImageSelected by rememberUpdatedState(viewModel::onImageSelected)

    var pendingToggleKanji by remember { mutableStateOf<Char?>(null) }
    var selectedKanji by remember { mutableStateOf<Char?>(null) }
    var googleKanji by remember { mutableStateOf<Char?>(null) }
    var detailsKanji by remember { mutableStateOf<Kanji?>(null) }
    val shouldShowImageScanHint by onboardingManager.shouldShowHint(IMAGE_SCAN_HINT_KEY)
        .collectAsStateWithLifecycle(initialValue = false)
    var imageScanHintDismissedInComposition by remember { mutableStateOf(false) }
    val showImageScanHint = shouldShowImageScanHint && !imageScanHintDismissedInComposition

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent(),
    ) { uri ->
        uri?.let(latestOnImageSelected)
    }

    LaunchedEffect(viewModel.uiEvent, galleryLauncher, context) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                ImageKanjiScanUiEvent.LaunchGalleryPicker -> galleryLauncher.launch("image/*")
                ImageKanjiScanUiEvent.KanjiDownloadNotCompleted -> Toast.makeText(
                    context,
                    "Please wait until the kanji download is complete before marking a kanji as learned.",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .safeDrawingPadding()
                    .navigationBarsPadding()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                HomeTopBar(
                    isDarkTheme = isDarkTheme,
                    showJlptFilter = false,
                    selectedJlptLevel = null,
                    onOpenDrawer = {
                        context.startActivity(Intent(context, HelpActivity::class.java))
                    },
                    onOpenJlptFilter = {},
                    onThemeChanged = onThemeChanged,
                    showBackButton = true,
                    onBackClick = { (context as? Activity)?.finish() },
                )

                ImageScanHeader()

                ImageSelectionCard(
                    selectedImageUri = uiState.selectedImageUri,
                    isLoading = uiState.isLoading,
                    imageSize = imageSize,
                    onSelectImage = viewModel::onScanImageClick,
                )

                ImageScanActions(
                    hasImage = uiState.selectedImageUri != null,
                    isLoading = uiState.isLoading,
                    onRetry = viewModel::retryScan,
                    onRemove = viewModel::removeImage,
                )

                ScanStatus(
                    isLoading = uiState.isLoading,
                    message = uiState.message,
                )

                RecognizedKanjiCard(
                    recognizedKanji = uiState.recognizedKanji,
                    learnedKanjis = uiState.learnedKanjis,
                    isLoading = uiState.isLoading,
                    onKanjiClick = { selectedKanji = it },
                    onKanjiLongClick = { kanji ->
                        clipboardManager.setText(AnnotatedString(kanji.toString()))
                    },
                    modifier = Modifier.padding(bottom = 20.dp),
                )
            }

            ImageScanDialogs(
                uiState = uiState,
                selectedKanji = selectedKanji,
                googleKanji = googleKanji,
                detailsKanji = detailsKanji,
                pendingToggleKanji = pendingToggleKanji,
                onSelectedKanjiChange = { selectedKanji = it },
                onGoogleKanjiChange = { googleKanji = it },
                onDetailsKanjiChange = { detailsKanji = it },
                onPendingToggleKanjiChange = { pendingToggleKanji = it },
                onCopyKanji = { kanji -> clipboardManager.setText(AnnotatedString(kanji.toString())) },
                onSearchKanji = { kanji ->
                    customTab.openCustomTab(context, googleAISearch(kanji.toString()))
                },
                onToggleLearned = viewModel::toggleLearnedKanji,
            )

            if (showImageScanHint) {
                OnboardingOverlay(
                    message = "Image Scanner\n\nUse this screen to scan kanji from images.\n\nTap the image card to select a picture from your device. After scanning, recognized kanji appear below. Tap a kanji for actions or long press it to copy.",
                    onDismiss = {
                        imageScanHintDismissedInComposition = true
                        coroutineScope.launch {
                            onboardingManager.markHintShown(IMAGE_SCAN_HINT_KEY)
                        }
                    },
                    textBottomPadding = 112.dp,
                )
            }
        }
    }
}

@Composable
private fun ImageScanDialogs(
    uiState: ImageScanUiState,
    selectedKanji: Char?,
    googleKanji: Char?,
    detailsKanji: Kanji?,
    pendingToggleKanji: Char?,
    onSelectedKanjiChange: (Char?) -> Unit,
    onGoogleKanjiChange: (Char?) -> Unit,
    onDetailsKanjiChange: (Kanji?) -> Unit,
    onPendingToggleKanjiChange: (Char?) -> Unit,
    onCopyKanji: (Char) -> Unit,
    onSearchKanji: (Char) -> Unit,
    onToggleLearned: (Char) -> Unit,
) {
    selectedKanji?.let { kanji ->
        val joyoKanji = uiState.recognizedJoyoKanjis[kanji]
        KanjiOptionsDialog(
            kanji = kanji,
            joyoKanji = joyoKanji,
            isLearned = uiState.learnedKanjis.contains(kanji),
            onDismiss = { onSelectedKanjiChange(null) },
            onCopy = {
                onCopyKanji(kanji)
                onSelectedKanjiChange(null)
            },
            onDetails = {
                onDetailsKanjiChange(it)
                onSelectedKanjiChange(null)
            },
            onSearch = {
                onGoogleKanjiChange(kanji)
                onSelectedKanjiChange(null)
            },
            onToggleLearned = {
                onPendingToggleKanjiChange(kanji)
                onSelectedKanjiChange(null)
            },
        )
    }

    googleKanji?.let { kanji ->
        GoogleSearchDialog(
            kanji = kanji,
            onConfirm = {
                onSearchKanji(kanji)
                onGoogleKanjiChange(null)
            },
            onDismiss = { onGoogleKanjiChange(null) },
        )
    }

    detailsKanji?.let { kanji ->
        val kanjiChar = kanji.kanji.first()
        KanjiDetailsDialog(
            kanji = kanji,
            isLearned = uiState.learnedKanjis.contains(kanjiChar),
            onDismiss = { onDetailsKanjiChange(null) },
            onSearch = {
                onSearchKanji(kanjiChar)
                onDetailsKanjiChange(null)
            },
            onToggleLearned = {
                onPendingToggleKanjiChange(kanjiChar)
                onDetailsKanjiChange(null)
            },
        )
    }

    pendingToggleKanji?.let { kanji ->
        ToggleLearnedDialog(
            kanji = kanji,
            isLearned = uiState.learnedKanjis.contains(kanji),
            onConfirm = {
                onToggleLearned(kanji)
                onPendingToggleKanjiChange(null)
                onDetailsKanjiChange(null)
                onSelectedKanjiChange(null)
            },
            onDismiss = { onPendingToggleKanjiChange(null) },
        )
    }
}
