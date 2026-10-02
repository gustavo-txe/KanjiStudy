package com.app.kanjistudy.data.image

import android.content.Context
import android.net.Uri
import com.app.kanjistudy.di.IoDispatcher
import com.google.mlkit.vision.common.InputImage
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext

class GalleryImageLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    private companion object {
        const val MAX_SCAN_IMAGE_SIZE = 2400
    }

    suspend fun load(uri: Uri): InputImage = withContext(dispatcher) {
        InputImage.fromBitmap(BoundedImageDecoder.decode(context, uri, MAX_SCAN_IMAGE_SIZE), 0)
    }
}
