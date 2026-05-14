package com.app.kanjistudy.scan

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.kanjistudy.home.kanjis.components.HomeTopBar
import com.app.kanjistudy.onboarding.OnboardingManager
import com.app.kanjistudy.onboarding.OnboardingOverlay
import com.app.kanjistudy.usecase.CustomTab
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ImageScanScreen(
    onboardingManager: OnboardingManager,
    isDarkTheme: Boolean,
    onThemeChanged: (Boolean) -> Unit,
    viewModel: ImageScanViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val documentScannerOptions = viewModel.documentScannerOptions
    val configuration = LocalConfiguration.current
    val customTab = remember { CustomTab() }
    val clipboardManager = LocalClipboardManager.current
    val imageSize = (configuration.screenWidthDp.dp - 40.dp).coerceIn(220.dp, 520.dp)
    var toggleLearnedKanji by remember { mutableStateOf<Char?>(null) }
    var optionsKanji by remember { mutableStateOf<Char?>(null) }
    var googleKanji by remember { mutableStateOf<Char?>(null) }
    var detailsKanji by remember { mutableStateOf<com.app.kanjistudy.data.model.KanjiData?>(null) }
    var showImageScanHint by remember { mutableStateOf(onboardingManager.shouldShowHint("image_scan")) }

    val scannerLauncher = rememberLauncherForActivityResult(StartIntentSenderForResult()) { result ->
        if (result.resultCode == RESULT_OK) {
            val scanningResult = GmsDocumentScanningResult.fromActivityResultIntent(result.data)
            scanningResult?.pages?.firstOrNull()?.imageUri?.let(viewModel::onImageSelected)
        }
    }

    val context = LocalContext.current

    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                ImageKanjiScanUiEvent.LaunchDocumentScanner -> {
                    GmsDocumentScanning.getClient(documentScannerOptions)
                        .getStartScanIntent(context as ComponentActivity)
                        .addOnSuccessListener { intentSender ->
                            scannerLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
                        }
                }
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
                    onThemeChanged = onThemeChanged,
                    showBackButton = true,
                    onBackClick = { (context as? Activity)?.finish() }
                )


            Text(
                text = "Scan an image or document to detect Kanji characters.",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                fontSize = 16.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(14.dp),
            )

            ElevatedCard(modifier = Modifier.size(imageSize)) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        .border(
                            1.dp,
                            MaterialTheme.colorScheme.outlineVariant,
                            RoundedCornerShape(20.dp)
                        )
                        .clickable(
                            enabled = !uiState.isLoading,
                            onClick = viewModel::onScanImageClick
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    if (uiState.selectedImageUri != null) {
                        AndroidView(
                            modifier = Modifier.fillMaxSize(),
                            factory = { androidContext ->
                                ImageView(androidContext).apply {
                                    scaleType = ImageView.ScaleType.CENTER_CROP
                                }
                            },
                            update = { it.setImageURI(uiState.selectedImageUri) },
                        )
                    } else {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Tap to scan an image or document",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp),
                            )
                            Text(
                                text = "You can crop and adjust before analyzing.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 24.dp).padding(top = 8.dp),
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = viewModel::retryScan,
                    enabled = uiState.selectedImageUri != null && !uiState.isLoading,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primaryContainer),
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = "Retry analysis",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }

                Spacer(modifier = Modifier.size(12.dp))

                OutlinedButton(
                    onClick = viewModel::removeImage,
                    enabled = uiState.selectedImageUri != null && !uiState.isLoading,
                ) {
                    Text("Remove image")
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            if (uiState.isLoading) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text("Analyzing image...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }

            uiState.message?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp),
                )
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth().fillMaxHeight().padding(bottom = 20.dp)
                    .height((configuration.screenHeightDp.dp * 0.32f).coerceIn(180.dp, 360.dp))
                    .padding(top = 8.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                if (uiState.recognizedKanji.isBlank() && !uiState.isLoading) {
                    Text(
                        text = "Detected Kanji will appear here.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                    )
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        uiState.recognizedKanji.toList().chunked(4).forEach { rowKanjis ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowKanjis.forEach { kanji ->
                                    Text(
                                        text = kanji.toString(),
                                        textAlign = TextAlign.Center,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 40.sp,
                                        lineHeight = 40.sp,
                                        color = if (uiState.learnedKanjis.contains(kanji)) androidx.compose.ui.graphics.Color(0xFF2E7D32) else MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier
                                            .weight(1f)
                                            .combinedClickable(
                                                onClick = { optionsKanji = kanji },
                                                onLongClick = { clipboardManager.setText(AnnotatedString(kanji.toString())) }
                                                    ),
                                    )
                                }
                                repeat(4 - rowKanjis.size) {
                                    Spacer(modifier = Modifier.weight(1f))
                                }
                            }
                        }
                    }
                }
            }
                optionsKanji?.let { kanji ->
                    val joyoKanji = uiState.recognizedJoyoKanjis[kanji]
                    val isLearned = uiState.learnedKanjis.contains(kanji)
                    AlertDialog(
                        onDismissRequest = { optionsKanji = null },
                        title = {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    StatusToggleIcon(
                                        isLearned = isLearned,
                                        onToggle = { toggleLearnedKanji = kanji }
                                    )
                                }
                                Text(
                                    text = "$kanji",
                                    modifier = Modifier.fillMaxWidth(),
                                    textAlign = TextAlign.Center,
                                    fontSize = 100.sp
                                )
                            }
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                googleKanji = kanji
                                optionsKanji = null
                            }) { Text("Search on Google") }
                        },
                        dismissButton = {
                            Row {
                                TextButton(onClick = {
                                    clipboardManager.setText(AnnotatedString(kanji.toString()))
                                    optionsKanji = null
                                }) { Text("Copy") }
                                if (joyoKanji != null) {
                                    TextButton(onClick = {
                                        detailsKanji = joyoKanji
                                        optionsKanji = null
                                    }) { Text("Details") }
                                }
                            }
                        }
                    )
                }

                googleKanji?.let { kanji ->
                    AlertDialog(
                        onDismissRequest = { googleKanji = null },
                        title = { Text("Open Google?") },
                        text = { Text("Would you like to search for $kanji on Google?") },
                        confirmButton = {
                            TextButton(onClick = {
                                customTab.openCustomTab(context, "https://www.google.com/search?q=kanji+$kanji")
                                googleKanji = null
                            }) { Text("Yes") }
                        },
                        dismissButton = { TextButton(onClick = { googleKanji = null }) { Text("No") } }
                    )
                }

                detailsKanji?.let { kanji ->
                    val isLearned = uiState.learnedKanjis.contains(kanji.kanji.first())
                    AlertDialog(
                        onDismissRequest = { detailsKanji = null },
                        title = {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                StatusToggleIcon(
                                    isLearned = isLearned,
                                    onToggle = { toggleLearnedKanji = kanji.kanji.first() }                                )
                            }
                        },                        text = {
                            Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(text = kanji.kanji, fontSize = 96.sp, lineHeight = 98.sp)
                                Row(modifier = Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.SpaceAround) {
                                    DetailColumn(title = "Kun'yomi:", items = kanji.kunReadings)
                                    DetailColumn(title = "On'yomi:", items = kanji.onReadings)
                                    DetailColumn(title = "Meanings:", items = kanji.meanings)
                                }
                            }
                        },
                        confirmButton = { TextButton(onClick = { detailsKanji = null }) { Text("Close") } }
                    )
                }
                toggleLearnedKanji?.let { kanji ->
                    val isLearned = uiState.learnedKanjis.contains(kanji)
                    AlertDialog(
                        onDismissRequest = { toggleLearnedKanji = null },
                        title = {
                            Text(if (isLearned) "Remove learned kanji?" else "Add kanji?")
                        },
                        text = {
                            Text(
                                if (isLearned) "Would you like to remove $kanji from learned?"
                                else "Would you like to add $kanji as learned?"
                            )
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                viewModel.toggleLearnedKanji(kanji)
                                toggleLearnedKanji = null
                            }) { Text("Yes") }
                        },
                        dismissButton = {
                            TextButton(onClick = { toggleLearnedKanji = null }) { Text("No") }
                        }
                    )
                }
        }

            if (showImageScanHint) {
                OnboardingOverlay(
                    message = "Image Scanner\n\nUse this screen to scan kanji from photos and documents.\n\nTap the large card to open the scanner. You can choose an image from your device or scan a document page, then crop and adjust before analysis.\n\nAfter scanning, recognized kanji appear below. Use retry to analyze again or remove image to start over.",
                    onDismiss = {
                        onboardingManager.markHintShown("image_scan")
                        showImageScanHint = false
                    },
                )
            }
        }
    }
}


@Composable
private fun DetailColumn(title: String, items: List<String>) {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        items.forEach { Text(text = it, fontSize = 13.sp) }
    }
}