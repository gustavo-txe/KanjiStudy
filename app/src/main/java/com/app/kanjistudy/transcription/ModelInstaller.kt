package com.app.kanjistudy.transcription

import android.content.Context
import java.io.File
import java.util.zip.ZipInputStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ModelInstaller(private val context: Context) {

    private val requiredEntries = setOf("am", "conf", "graph", "ivector")
    private val modelDir = File(context.filesDir, "vosk-model-small-ja-0.22")

    suspend fun ensureInstalled(): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            if (!isInstalled()) {
                reinstallInternal()
            }
            modelDir
        }
    }

    suspend fun reinstall(): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            reinstallInternal()
            modelDir
        }
    }

    fun isInstalled(): Boolean = requiredEntries.all { File(modelDir, it).exists() }

    private fun reinstallInternal() {
        if (modelDir.exists()) modelDir.deleteRecursively()
        val tempZip = File(context.cacheDir, "vosk-model-small-ja-0.22.zip")
        context.assets.open("vosk-model-small-ja-0.22.zip").use { input ->
            tempZip.outputStream().use { output -> input.copyTo(output) }
        }
        unzip(tempZip, context.filesDir)
        tempZip.delete()
        if (!isInstalled()) {
            modelDir.deleteRecursively()
            throw IllegalStateException("Model validation failed after install")
        }
    }

    private fun unzip(zipFile: File, targetDir: File) {
        ZipInputStream(zipFile.inputStream()).use { zis ->
            var entry = zis.nextEntry
            while (entry != null) {
                val outFile = File(targetDir, entry.name)
                if (entry.isDirectory) {
                    outFile.mkdirs()
                } else {
                    outFile.parentFile?.mkdirs()
                    outFile.outputStream().use { output -> zis.copyTo(output) }
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
    }
}