package com.app.kanjistudy.data.backup

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class KanjiAutoBackup @Inject constructor(
    @ApplicationContext private val context: Context
) {

    private val fileMutex = Mutex()

    suspend fun readBackupJson(): String? = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            val backupFile = getBackupFile()
            if (!backupFile.exists()) return@withLock null
            backupFile.readText(Charsets.UTF_8)
        }
    }

    suspend fun writeBackupJson(json: String) = withContext(Dispatchers.IO) {
        fileMutex.withLock {
            getBackupFile().writeText(json, Charsets.UTF_8)
        }
    }

    private fun getBackupFile(): File = File(context.filesDir, FILE_NAME)

    private companion object {
        const val FILE_NAME = "learned_kanji_auto_backup.json"
    }
}