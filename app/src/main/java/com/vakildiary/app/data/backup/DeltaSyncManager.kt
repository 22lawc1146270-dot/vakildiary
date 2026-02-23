package com.vakildiary.app.data.backup

import android.content.Context
import com.google.api.client.http.FileContent
import com.google.api.services.drive.model.File
import com.vakildiary.app.core.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.FileInputStream
import java.io.File as IoFile
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DeltaSyncManager @Inject constructor(
    private val context: Context,
    private val driveBackupManager: DriveBackupManager,
    private val checksumStore: ChecksumStore,
    private val driveAuthManager: DriveAuthManager
) {
    suspend fun syncDocuments(): Result<Int> = withContext(Dispatchers.IO) {
        when (val authResult = driveAuthManager.ensureInitialized()) {
            is Result.Error -> return@withContext Result.Error(authResult.message, authResult.throwable)
            is Result.Success -> Unit
        }

        val documentsDir = IoFile(context.filesDir, DOCUMENTS_DIR)
        if (!documentsDir.exists()) return@withContext Result.Success(0)

        val currentChecksums = mutableMapOf<String, String>()
        val previousChecksums = checksumStore.getChecksumMap()

        val changedFiles = mutableListOf<IoFile>()

        documentsDir.walkTopDown().filter { it.isFile }.forEach { file ->
            val checksum = sha256(file)
            currentChecksums[file.absolutePath] = checksum
            val previous = previousChecksums[file.absolutePath]
            if (previous == null || previous != checksum) {
                changedFiles.add(file)
            }
        }

        var uploaded = 0
        for (file in changedFiles) {
            val result = driveBackupManager.uploadBackup(file, mimeType = "application/octet-stream")
            if (result is Result.Success) uploaded++
        }

        checksumStore.saveChecksumMap(currentChecksums)
        Result.Success(uploaded)
    }

    private fun sha256(file: IoFile): String {
        val digest = MessageDigest.getInstance("SHA-256")
        FileInputStream(file).use { input ->
            val buffer = ByteArray(8192)
            var read = input.read(buffer)
            while (read != -1) {
                digest.update(buffer, 0, read)
                read = input.read(buffer)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val DOCUMENTS_DIR = "documents"
    }
}
