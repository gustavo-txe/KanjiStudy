package com.app.kanjistudy.data.document

import android.content.Context
import android.net.Uri
import com.app.kanjistudy.di.IoDispatcher
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

class JsonDocumentStore @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val dispatcher: CoroutineDispatcher,
) {
    private companion object {
        const val MAX_DOCUMENT_CHARACTERS = 1_048_576
    }

    suspend fun read(uri: Uri): String = withContext(dispatcher) {
        context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { reader ->
            val result = StringBuilder()
            val buffer = CharArray(8_192)
            while (true) {
                currentCoroutineContext().ensureActive()
                val count = reader.read(buffer)
                if (count < 0) break
                require(result.length + count <= MAX_DOCUMENT_CHARACTERS) { "Backup is too large" }
                result.append(buffer, 0, count)
            }
            result.toString()
        }
            ?: throw IOException("Unable to open the selected file.")
    }

    suspend fun write(uri: Uri, json: String): Unit = withContext(dispatcher) {
        context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            ?: throw IOException("Unable to open the selected file.")
    }
}
