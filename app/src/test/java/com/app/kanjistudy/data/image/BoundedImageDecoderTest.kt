package com.app.kanjistudy.data.image

import android.app.Application
import android.graphics.Bitmap
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [26, 28])
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class BoundedImageDecoderTest {
    @Test fun `JPEG rotation is preserved after bounded decoding`() {
        val context = RuntimeEnvironment.getApplication()
        val file = File(context.cacheDir, "rotated-image.jpg")
        val source = Bitmap.createBitmap(600, 300, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { source.compress(Bitmap.CompressFormat.JPEG, 90, it) }
        } finally {
            source.recycle()
        }
        try {
            ExifInterface(file).apply {
                setAttribute(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_ROTATE_90.toString())
                saveAttributes()
            }
            val decoded = BoundedImageDecoder.decode(context, Uri.fromFile(file), 1200)
            try {
                assertEquals(300, decoded.width)
                assertEquals(600, decoded.height)
            } finally {
                decoded.recycle()
            }
        } finally {
            file.delete()
        }
    }

    @Test fun `large source is decoded within preview and OCR memory bounds`() {
        val context = RuntimeEnvironment.getApplication()
        val file = File(context.cacheDir, "large-image.png")
        val source = Bitmap.createBitmap(4800, 1200, Bitmap.Config.ARGB_8888)
        try {
            file.outputStream().use { source.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } finally {
            source.recycle()
        }
        try {
            for (maxEdge in listOf(1200, 2400)) {
                val decoded = BoundedImageDecoder.decode(context, Uri.fromFile(file), maxEdge)
                try {
                    assertTrue(maxOf(decoded.width, decoded.height) <= maxEdge)
                    assertEquals(4, decoded.width / decoded.height)
                    assertTrue(decoded.allocationByteCount <= maxEdge * maxEdge * 4)
                } finally {
                    decoded.recycle()
                }
            }
        } finally {
            file.delete()
        }
    }
}
