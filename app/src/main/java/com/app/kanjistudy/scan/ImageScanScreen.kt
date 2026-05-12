package com.app.kanjistudy.scan

import android.app.Activity
import android.app.Activity.RESULT_OK
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.kanjistudy.onboarding.OnboardingManager
import com.app.kanjistudy.onboarding.OnboardingOverlay
import com.google.mlkit.vision.documentscanner.GmsDocumentScanning
import com.google.mlkit.vision.documentscanner.GmsDocumentScanningResult

@Composable
fun ImageScanScreen(onboardingManager: OnboardingManager,
                    viewModel: ImageScanViewModel = hiltViewModel(),) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val documentScannerOptions = viewModel.documentScannerOptions
    val configuration = LocalConfiguration.current
    val imageSize = (configuration.screenWidthDp.dp - 40.dp).coerceIn(220.dp, 520.dp)
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
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
                IconButton(onClick = { (context as? Activity)?.finish() }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Go back",
                        tint = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }

            Text(
                text = "Image Kanji Scanner",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = "Scan an image or document to detect Kanji characters.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp, bottom = 12.dp),
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
                    .fillMaxWidth()
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
                    Text(
                        text = uiState.recognizedKanji.toCharArray().joinToString("   "),
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Bold,
                        fontSize = 34.sp,
                        lineHeight = 46.sp,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
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