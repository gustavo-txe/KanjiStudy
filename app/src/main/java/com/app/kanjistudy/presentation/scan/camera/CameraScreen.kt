package com.app.kanjistudy.presentation.scan.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.kanjistudy.R
import com.app.kanjistudy.core.navigation.CustomTab
import com.app.kanjistudy.core.ui.UserMessage
import com.app.kanjistudy.data.preferences.OnboardingManager
import com.app.kanjistudy.domain.model.Kanji
import com.app.kanjistudy.presentation.scan.camera.components.CameraScanContent
import com.app.kanjistudy.presentation.scan.camera.components.CameraScanDialogs
import com.app.kanjistudy.presentation.scan.camera.components.ScanImageButton
import com.app.kanjistudy.presentation.scan.image.ImageKanjiScanActivity

@Composable
fun ScanScreen(
    viewModel: CameraScanViewModel = hiltViewModel(),
    onboardingManager: OnboardingManager,
) {
    val context = LocalContext.current
    CameraPermission(
        onScanImage = {
            context.startActivity(Intent(context, ImageKanjiScanActivity::class.java))
        },
    ) {
        CameraScreen(viewModel = viewModel, onboardingManager = onboardingManager)
    }
}

@Composable
fun CameraScreen(
    viewModel: CameraScanViewModel = hiltViewModel(),
    onboardingManager: OnboardingManager,
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    UserMessage(uiState.userMessage, viewModel::clearUserMessage)
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val customTab = remember { CustomTab() }

    var selectedKanji by remember { mutableStateOf<Char?>(null) }
    var googleSearchKanji by remember { mutableStateOf<String?>(null) }
    var detailsKanji by remember { mutableStateOf<Kanji?>(null) }
    var learnedToggleKanji by remember { mutableStateOf<Char?>(null) }

    LaunchedEffect(viewModel) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ScanUiEvent.CopyKanji -> {
                    clipboardManager.setText(AnnotatedString(event.kanji.toString()))
                    Toast.makeText(context, context.getString(R.string.kanji_copied), Toast.LENGTH_SHORT).show()
                }

                ScanUiEvent.KanjiDownloadNotCompleted -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.progress_download_pending),
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
            onCameraError = viewModel::onCameraError,
            onScanImage = {
                context.startActivity(Intent(context, ImageKanjiScanActivity::class.java))
            },
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
    onScanImage: () -> Unit,
    onPermissionGranted: @Composable () -> Unit,
) {
    val context = LocalContext.current
    var permissionRequested by rememberSaveable { mutableStateOf(false) }
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

    LifecycleEventEffect(Lifecycle.Event.ON_START) {
        permissionGranted = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA,
        ) == PackageManager.PERMISSION_GRANTED
        if (!permissionGranted && !permissionRequested) {
            permissionRequested = true
            launcher.launch(Manifest.permission.CAMERA)
        }
    }

    if (permissionGranted) {
        onPermissionGranted()
    } else {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background,
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(start = 24.dp, end = 24.dp, bottom = 96.dp)
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Text(
                        text = stringResource(R.string.camera_permission_required),
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                    )
                    Button(
                        onClick = {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                    Uri.fromParts("package", context.packageName, null),
                                ),
                            )
                        },
                    ) {
                        Text(stringResource(R.string.camera_permission_settings))
                    }
                }
                ScanImageButton(
                    modifier = Modifier.align(Alignment.BottomStart).padding(20.dp),
                    onClick = onScanImage,
                )
            }
        }
    }
}
