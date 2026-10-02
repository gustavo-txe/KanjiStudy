package com.app.kanjistudy.data.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import android.net.Uri
import android.os.Build
import java.io.IOException
import kotlin.math.roundToInt

/** Bounds allocation during decoding, including on Android 8. */
internal object BoundedImageDecoder {
    fun decode(context: Context, uri: Uri, maxEdge: Int): Bitmap {
        require(maxEdge > 0)
        if (Build.VERSION.SDK_INT >= 28) {
            return ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val scale = minOf(1.0, maxEdge.toDouble() / maxOf(info.size.width, info.size.height))
                decoder.setTargetSize(
                    (info.size.width * scale).roundToInt().coerceAtLeast(1),
                    (info.size.height * scale).roundToInt().coerceAtLeast(1)
                )
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
            }
        }
        val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open(context, uri).use { BitmapFactory.decodeStream(it, null, options) }
        if (options.outWidth <= 0 || options.outHeight <= 0) throw IOException("Invalid image dimensions")
        options.inSampleSize = sampleSize(options.outWidth, options.outHeight, maxEdge)
        options.inJustDecodeBounds = false
        val bitmap = open(context, uri).use { BitmapFactory.decodeStream(it, null, options) }
            ?: throw IOException("Unable to decode image")
        val orientation = try {
            open(context, uri).use { ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL) }
        } catch (_: IOException) {
            ExifInterface.ORIENTATION_NORMAL
        }
        val matrix = Matrix().apply {
            when (orientation) {
                ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> setScale(-1f, 1f)
                ExifInterface.ORIENTATION_ROTATE_180 -> setRotate(180f)
                ExifInterface.ORIENTATION_FLIP_VERTICAL -> setScale(1f, -1f)
                ExifInterface.ORIENTATION_TRANSPOSE -> { setRotate(90f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_90 -> setRotate(90f)
                ExifInterface.ORIENTATION_TRANSVERSE -> { setRotate(270f); postScale(-1f, 1f) }
                ExifInterface.ORIENTATION_ROTATE_270 -> setRotate(270f)
            }
        }
        if (matrix.isIdentity) return bitmap
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true).also {
            if (it !== bitmap) bitmap.recycle()
        }
    }

    internal fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        while ((maxOf(width, height).toLong() + sample - 1) / sample > maxEdge) sample *= 2
        return sample
    }

    private fun open(context: Context, uri: Uri) = context.contentResolver.openInputStream(uri)
        ?: throw IOException("Unable to open image")
}
