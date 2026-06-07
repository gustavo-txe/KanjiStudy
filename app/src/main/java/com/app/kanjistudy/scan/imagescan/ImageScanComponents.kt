package com.app.kanjistudy.scan.imagescan

import android.content.Context
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddPhotoAlternate
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.ImageSearch
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
internal fun ImageScanHeader() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
    ) {
        Text(
            text = "Image Scanner",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Select an image and detect Japanese kanji without blocking your study flow.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp),
        )
    }
}

@Composable
internal fun ImageSelectionCard(
    selectedImageUri: Uri?,
    isLoading: Boolean,
    imageSize: Dp,
    onSelectImage: () -> Unit,
) {
    ElevatedCard(
        modifier = Modifier.size(imageSize),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 4.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(28.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .border(
                    width = 1.dp,
                    color = MaterialTheme.colorScheme.outlineVariant,
                    shape = RoundedCornerShape(28.dp),
                )
                .clickable(enabled = !isLoading, onClick = onSelectImage),
            contentAlignment = Alignment.Center,
        ) {
            selectedImageUri?.let { uri ->
                AsyncUriImage(
                    uri = uri,
                    modifier = Modifier.fillMaxSize(),
                )
            } ?: EmptyImageState()
        }
    }
}

@Composable
private fun EmptyImageState() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.padding(24.dp),
    ) {
        Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        ) {
            Icon(
                imageVector = Icons.Rounded.AddPhotoAlternate,
                contentDescription = null,
                modifier = Modifier.padding(16.dp),
            )
        }
        Text(
            text = "Tap to select an image",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Clear, high-contrast images produce the best results.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun AsyncUriImage(uri: Uri, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    var imageState by remember(uri) { mutableStateOf<AsyncImageState>(AsyncImageState.Loading) }

    LaunchedEffect(uri) {
        imageState = AsyncImageState.Loading
        imageState = loadImageBitmap(context, uri)
    }

    when (val state = imageState) {
        AsyncImageState.Loading -> CircularProgressIndicator()
        AsyncImageState.Error -> Text(
            text = "Preview unavailable",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        is AsyncImageState.Success -> Image(
            bitmap = state.bitmap,
            contentDescription = "Selected image preview",
            contentScale = ContentScale.Crop,
            modifier = modifier,
        )
    }
}

private suspend fun loadImageBitmap(context: Context, uri: Uri): AsyncImageState = withContext(Dispatchers.IO) {
    runCatching {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(context.contentResolver, uri)
            ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
                val sample = calculateSampleSize(info.size.width, info.size.height)
                decoder.setTargetSampleSize(sample)
            }
        } else {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                BitmapFactory.decodeStream(stream)
            } ?: error("Unable to open image stream")
        }
        AsyncImageState.Success(bitmap.asImageBitmap())
    }.getOrElse { error ->
        if (error is CancellationException) throw error
        AsyncImageState.Error
    }
}

private fun calculateSampleSize(width: Int, height: Int): Int {
    val longestEdge = maxOf(width, height).coerceAtLeast(1)
    return (longestEdge / MAX_PREVIEW_IMAGE_SIZE).coerceAtLeast(1)
}

private const val MAX_PREVIEW_IMAGE_SIZE = 1_200

private sealed interface AsyncImageState {
    data object Loading : AsyncImageState
    data object Error : AsyncImageState
    data class Success(val bitmap: androidx.compose.ui.graphics.ImageBitmap) : AsyncImageState
}

@Composable
internal fun ImageScanActions(
    hasImage: Boolean,
    isLoading: Boolean,
    onRetry: () -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 14.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalIconButton(
            onClick = onRetry,
            enabled = hasImage && !isLoading,
            modifier = Modifier.size(56.dp),
        ) {
            Icon(
                imageVector = Icons.Rounded.Refresh,
                contentDescription = "Retry analysis",
            )
        }

        Spacer(modifier = Modifier.size(12.dp))

        OutlinedButton(
            onClick = onRemove,
            enabled = hasImage && !isLoading,
        ) {
            Icon(
                imageVector = Icons.Rounded.Close,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text("Remove image")
        }
    }
}

@Composable
internal fun ScanStatus(isLoading: Boolean, message: String?) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = "Analyzing image...",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        message?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
internal fun RecognizedKanjiCard(
    recognizedKanji: String,
    learnedKanjis: Set<Char>,
    isLoading: Boolean,
    onKanjiClick: (Char) -> Unit,
    onKanjiLongClick: (Char) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(260.dp)
                .verticalScroll(rememberScrollState())
                .padding(18.dp),
            contentAlignment = Alignment.Center,
        ) {
            if (recognizedKanji.isBlank() && !isLoading) {
                Text(
                    text = "Detected kanji will appear here.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    recognizedKanji.toList().chunked(KANJI_PER_ROW).forEach { rowKanjis ->
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(
                                space = 14.dp,
                                alignment = Alignment.CenterHorizontally,
                            ),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            rowKanjis.forEach { kanji ->
                                KanjiResultChip(
                                    kanji = kanji,
                                    isLearned = learnedKanjis.contains(kanji),
                                    onClick = { onKanjiClick(kanji) },
                                    onLongClick = { onKanjiLongClick(kanji) },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun KanjiResultChip(
    kanji: Char,
    isLearned: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val containerColor = if (isLearned) {
        MaterialTheme.colorScheme.surface
    } else {
        MaterialTheme.colorScheme.surface
    }
    val contentColor = if (isLearned) {
        Color(0xFF4CAF50)
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = containerColor,
        tonalElevation = 2.dp,
        shadowElevation = 1.dp,
        modifier = Modifier.combinedClickable(
            onClick = onClick,
            onLongClick = onLongClick,
        ),
    ) {
        Text(
            text = kanji.toString(),
            color = contentColor,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
            fontSize = 42.sp,
            lineHeight = 44.sp,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
        )
    }
}

@Composable
internal fun DetailColumn(title: String, items: List<String>) {
    Column(modifier = Modifier.padding(horizontal = 4.dp)) {
        Text(text = title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
        items.forEach { Text(text = it, fontSize = 13.sp) }
    }
}

@Composable
internal fun DialogActionIcon(
    type: DialogActionIconType,
    contentDescription: String?,
) {
    Icon(
        imageVector = when (type) {
            DialogActionIconType.Copy -> Icons.Rounded.ContentCopy
            DialogActionIconType.Search -> Icons.Rounded.Search
        },
        contentDescription = contentDescription,
        modifier = Modifier.size(18.dp),
    )
}

internal enum class DialogActionIconType { Copy, Search }

private const val KANJI_PER_ROW = 4