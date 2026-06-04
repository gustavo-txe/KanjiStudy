package com.app.kanjistudy.data.backup

import android.app.backup.BackupManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.OutputStreamWriter
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.StandardCopyOption.ATOMIC_MOVE
import java.nio.file.StandardCopyOption.REPLACE_EXISTING
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KanjiAutoBackup @Inject constructor(
    @ApplicationContext context: Context
) {

    private val appContext = context.applicationContext
    private val fileMutex = Mutex()
    private val backupManager by lazy(LazyThreadSafetyMode.NONE) { BackupManager(appContext) }

    suspend fun readBackupJson(): String? = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val backupFile = getBackupFile()
            if (!backupFile.isFile || backupFile.length() == 0L) return@withLock null

            backupFile.inputStream().bufferedReader(Charsets.UTF_8).use { reader ->
                reader.readText()
            }
        }
    }

    suspend fun writeBackupJson(json: String) = withContext(Dispatchers.IO) {
        val didChange = fileMutex.withLock {
            val backupFile = getBackupFile()
            if (backupFile.isFile && backupFile.readText(Charsets.UTF_8) == json) {
                return@withLock false
            }

            val tempFile = File(appContext.filesDir, TEMP_FILE_NAME)
            try {
                writeTextDurably(tempFile, json)
                replaceBackupFile(tempFile, backupFile)
            } finally {
                if (tempFile.exists()) {
                    tempFile.delete()
                }
            }
            true
        }

        if (didChange) {
            backupManager.dataChanged()
        }
    }

    private fun writeTextDurably(file: File, text: String) {
        FileOutputStream(file).use { outputStream ->
            OutputStreamWriter(outputStream, Charsets.UTF_8).use { writer ->
                writer.write(text)
                writer.flush()
                outputStream.fd.sync()
            }
        }
    }

    private fun replaceBackupFile(source: File, destination: File) {
        runCatching {
            Files.move(source.toPath(), destination.toPath(), ATOMIC_MOVE, REPLACE_EXISTING)
        }.getOrElse { throwable ->
            if (throwable !is AtomicMoveNotSupportedException) throw throwable
            Files.move(source.toPath(), destination.toPath(), REPLACE_EXISTING)
        }
    }

    private fun getBackupFile(): File = File(appContext.filesDir, FILE_NAME)

    private companion object {
        const val FILE_NAME = "learned_kanji_auto_backup.json"
        const val TEMP_FILE_NAME = "$FILE_NAME.tmp"
    }
}