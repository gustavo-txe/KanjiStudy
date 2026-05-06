package com.app.kanjistudy.transcription

import android.net.Uri
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView

@RequiresApi(Build.VERSION_CODES.Q)
@Composable
fun TranscriptionScreen(
    onBack: () -> Unit,
    viewModel: TranscriptionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scrollState = rememberScrollState()
    val player = remember {
        ExoPlayer.Builder(context).build().apply { playWhenReady = false }
    }

    DisposableEffect(Unit) { onDispose { player.release() } }

    LaunchedEffect(uiState.selectedUri) {
        uiState.selectedUri?.let { uri ->
            player.setMediaItem(MediaItem.fromUri(uri))
            player.prepare()
            player.seekTo(uiState.clipStartMs)
        }
    }

    LaunchedEffect(uiState.clipStartMs, uiState.clipEndMs, uiState.isVideoFile) {
        if (uiState.isVideoFile && uiState.selectedUri != null) {
            player.seekTo(uiState.clipStartMs)
        }
    }

    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onMediaSelected(it) }
    }

    LaunchedEffect(Unit) { viewModel.initialize() }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TextButton(onClick = onBack, modifier = Modifier.align(Alignment.Start)) { Text("Back") }
            Text("Japanese Audio/Video Transcription", style = MaterialTheme.typography.headlineSmall, textAlign = TextAlign.Center)
            Text("Choose an audio or video file in Japanese, select up to 2 minutes, and start transcription.", textAlign = TextAlign.Center)

            when {
                uiState.isCheckingPrerequisites || uiState.isInstallingModel -> CircularProgressIndicator()
                uiState.prerequisiteMessage != null -> Text(uiState.prerequisiteMessage!!, textAlign = TextAlign.Center)
                else -> {
                    Button(onClick = { picker.launch("*/*") }) { Text("Select Audio/Video") }
                    Button(onClick = { viewModel.reinstallModel() }) { Text("Reinstall Model") }

                    Card(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                        if (uiState.selectedUri == null) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("No file selected") }
                        } else if (uiState.isVideoFile) {
                            AndroidView(
                                modifier = Modifier.fillMaxSize(),
                                factory = { PlayerView(it).apply { this.player = player; useController = true } }
                            )
                        } else {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Audio file selected") }
                        }
                    }

                    if (uiState.mediaDurationMs > 0) {
                        val maxEnd = minOf(uiState.mediaDurationMs, 120_000L)
                        Text("Start: ${uiState.clipStartMs / 1000}s")
                        Slider(
                            value = uiState.clipStartMs.toFloat(),
                            onValueChange = { viewModel.onClipChange(it.toLong(), uiState.clipEndMs.coerceAtLeast(it.toLong())) },
                            valueRange = 0f..uiState.clipEndMs.toFloat()
                        )
                        Text("End: ${uiState.clipEndMs / 1000}s (max 120s)")
                        Slider(
                            value = uiState.clipEndMs.toFloat(),
                            onValueChange = { viewModel.onClipChange(uiState.clipStartMs.coerceAtMost(it.toLong()), it.toLong()) },
                            valueRange = uiState.clipStartMs.toFloat()..maxEnd.toFloat()
                        )
                    }

                    Button(onClick = { viewModel.transcribe() }, enabled = uiState.selectedUri != null && !uiState.isTranscribing && uiState.isModelReady) {
                        Text(if (uiState.isTranscribing) "Transcribing..." else "Start Transcription")
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(280.dp)
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(12.dp)
                            .verticalScroll(rememberScrollState()),
                        contentAlignment = Alignment.TopCenter
                    ) {
                        Text(text = uiState.transcript.ifBlank { "Transcription will appear here." }, textAlign = TextAlign.Center)
                    }

                    uiState.error?.let { Text(text = it, color = MaterialTheme.colorScheme.error, textAlign = TextAlign.Center) }
                }
            }
        }

        if (uiState.isTranscribing) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.65f))
                    .clickable(enabled = true, onClick = {}),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("Please wait until the transcription is finished.", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        }
    }
}