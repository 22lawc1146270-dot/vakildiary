package com.vakildiary.app.data.backup

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import com.google.api.services.drive.model.File
import com.vakildiary.app.core.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class RestoreManager @Inject constructor(
    private val context: Context,
    private val driveBackupManager: DriveBackupManager
) {
    suspend fun hasLocalData(): Boolean = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        dbFile.exists() && dbFile.length() > 0
    }

    suspend fun hasRemoteBackup(): Result<Boolean> = withContext(Dispatchers.IO) {
        val listResult = driveBackupManager.listBackups(BACKUP_QUERY)
        return@withContext when (listResult) {
            is Result.Success -> Result.Success(listResult.data.isNotEmpty())
            is Result.Error -> Result.Error(listResult.message, listResult.throwable)
        }
    }

    suspend fun restoreLatestBackup(): Result<Unit> = withContext(Dispatchers.IO) {
        val listResult = driveBackupManager.listBackups(BACKUP_QUERY)
        val files = when (listResult) {
            is Result.Success -> listResult.data
            is Result.Error -> return@withContext Result.Error(listResult.message, listResult.throwable)
        }

        val latest = files.maxByOrNull { it.modifiedTime?.value ?: 0L }
            ?: return@withContext Result.Error("No backup found")

        val tempZip = File(context.cacheDir, "restore_backup.zip")
        val downloadResult = driveBackupManager.downloadBackup(latest.id, tempZip)
        if (downloadResult is Result.Error) {
            return@withContext Result.Error(downloadResult.message, downloadResult.throwable)
        }

        return@withContext unzipAndRestore(tempZip)
    }

    private fun unzipAndRestore(zipFile: File): Result<Unit> {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        val documentsDir = File(context.filesDir, DOCUMENTS_DIR)
        val dataStoreFile = context.preferencesDataStoreFile(DATASTORE_NAME)

        return try {
            ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zipIn ->
                var entry: ZipEntry? = zipIn.nextEntry
                while (entry != null) {
                    val name = entry.name
                    when {
                        name.startsWith("database/") -> {
                            dbFile.parentFile?.mkdirs()
                            writeEntry(zipIn, dbFile)
                        }
                        name.startsWith("documents/") -> {
                            val relative = name.removePrefix("documents/")
                            if (relative.isNotBlank()) {
                                val target = File(documentsDir, relative)
                                target.parentFile?.mkdirs()
                                writeEntry(zipIn, target)
                            }
                        }
                        name.startsWith("settings/") -> {
                            dataStoreFile.parentFile?.mkdirs()
                            writeEntry(zipIn, dataStoreFile)
                        }
                    }
                    zipIn.closeEntry()
                    entry = zipIn.nextEntry
                }
            }
            Result.Success(Unit)
        } catch (t: Throwable) {
            Result.Error("Restore failed", t)
        }
    }

    private fun writeEntry(zipIn: ZipInputStream, file: File) {
        BufferedOutputStream(FileOutputStream(file)).use { output ->
            zipIn.copyTo(output)
        }
    }

    companion object {
        private const val DATABASE_NAME = "vakildiary.db"
        private const val DOCUMENTS_DIR = "documents"
        private const val DATASTORE_NAME = "user_prefs"
        private const val BACKUP_QUERY = "name contains 'vakildiary_backup_' and mimeType='application/zip'"
    }
}
