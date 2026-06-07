package com.app.kanjistudy.scan.camerascan

import android.Manifest
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.kanjistudy.data.model.KanjiData
import com.app.kanjistudy.onboarding.OnboardingManager
import com.app.kanjistudy.scan.camerascan.components.CameraScanContent
import com.app.kanjistudy.scan.camerascan.components.CameraScanDialogs
import com.app.kanjistudy.usecase.CustomTab

@Composable
fun ScanScreen(
    viewModel: CameraScanViewModel = hiltViewModel(),
    onboardingManager: OnboardingManager,
) {
    CameraPermission {
        CameraScreen(viewModel = viewModel, onboardingManager = onboardingManager)
    }
}

@Composable
fun CameraScreen(
    viewModel: CameraScanViewModel = hiltViewModel(),
    onboardingManager: OnboardingManager,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val customTab = remember { CustomTab() }

    var selectedKanji by remember { mutableStateOf<Char?>(null) }
    var googleSearchKanji by remember { mutableStateOf<String?>(null) }
    var detailsKanji by remember { mutableStateOf<KanjiData?>(null) }
    var learnedToggleKanji by remember { mutableStateOf<Char?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ScanUiEvent.CopyKanji -> {
                    clipboardManager.setText(AnnotatedString(event.kanji.toString()))
                    Toast.makeText(context, "Kanji copied!", Toast.LENGTH_SHORT).show()
                }

                ScanUiEvent.KanjiDownloadNotCompleted -> {
                    Toast.makeText(
                        context,
                        "Please wait until the kanji download is complete before marking a kanji as learned.",
                        Toast.LENGTH_SHORT,
                    ).show()
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background,
    ) {
        CameraScanContent(
            uiState = uiState,
            onboardingManager = onboardingManager,
            onKanjiClick = { selectedKanji = it },
            onPauseToggle = viewModel::togglePause,
            onFrame = viewModel::onFrame,
        )
    }

    CameraScanDialogs(
        selectedKanji = selectedKanji,
        googleSearchKanji = googleSearchKanji,
        detailsKanji = detailsKanji,
        learnedToggleKanji = learnedToggleKanji,
        uiState = uiState,
        onDismissSelected = { selectedKanji = null },
        onDismissGoogleSearch = { googleSearchKanji = null },
        onConfirmGoogleSearch = { kanji ->
            customTab.openCustomTab(context, viewModel.googleAiSearchUrl(kanji))
            googleSearchKanji = null
        },
        onDismissDetails = { detailsKanji = null },
        onDismissLearnedToggle = { learnedToggleKanji = null },
        onCopyKanji = viewModel::onKanjiLongClick,
        onOpenGoogleConfirmation = { googleSearchKanji = it },
        onOpenDetails = { detailsKanji = it },
        onLearnedToggleRequest = { learnedToggleKanji = it },
        onConfirmLearnedToggle = viewModel::toggleLearnedKanji,
    )
}

@Composable
fun CameraPermission(
    onPermissionGranted: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.CAMERA,
            ) == PackageManager.PERMISSION_GRANTED,
        )
    }

    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { isGranted ->
        permissionGranted = isGranted
    }

    LaunchedEffect(permissionGranted) {
        if (!permissionGranted) {
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (permissionGranted) {
        onPermissionGranted()
    } else {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "Camera permission is required to scan kanji.",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }
    }
}