package com.vakildiary.app.data.backup

import android.content.Context
import androidx.datastore.preferences.preferencesDataStoreFile
import com.vakildiary.app.core.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManualBackupManager @Inject constructor(
    private val context: Context,
    private val driveBackupManager: DriveBackupManager
) {
    suspend fun backupNow(): Result<String> = withContext(Dispatchers.IO) {
        val dbFile = context.getDatabasePath(DATABASE_NAME)
        if (!dbFile.exists()) {
            return@withContext Result.Error("Database file not found")
        }

        val documentsDir = File(context.filesDir, DOCUMENTS_DIR)
        val dataStoreFile = context.preferencesDataStoreFile(DATASTORE_NAME)

        val backupFile = createBackupZip(dbFile, documentsDir, dataStoreFile)
            ?: return@withContext Result.Error("Failed to create backup file")

        return@withContext driveBackupManager.uploadBackup(
            file = backupFile,
            mimeType = "application/zip",
            folderId = null
        )
    }

    private fun createBackupZip(
        dbFile: File,
        documentsDir: File,
        dataStoreFile: File
    ): File? {
        val backupDir = File(context.cacheDir, "backups")
        if (!backupDir.exists()) backupDir.mkdirs()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val backupFile = File(backupDir, "vakildiary_backup_$timestamp.zip")

        return try {
            ZipOutputStream(BufferedOutputStream(FileOutputStream(backupFile))).use { zipOut ->
                addFileToZip(zipOut, dbFile, "database/${dbFile.name}")

                if (documentsDir.exists() && documentsDir.isDirectory) {
                    documentsDir.walkTopDown().filter { it.isFile }.forEach { file ->
                        val relative = file.relativeTo(documentsDir).path
                        addFileToZip(zipOut, file, "documents/$relative")
                    }
                }

                if (dataStoreFile.exists()) {
                    addFileToZip(zipOut, dataStoreFile, "settings/${dataStoreFile.name}")
                }
            }
            backupFile
        } catch (t: Throwable) {
            null
        }
    }

    private fun addFileToZip(zipOut: ZipOutputStream, file: File, entryName: String) {
        val entry = ZipEntry(entryName)
        zipOut.putNextEntry(entry)
        BufferedInputStream(FileInputStream(file)).use { input ->
            input.copyTo(zipOut)
        }
        zipOut.closeEntry()
    }

    companion object {
        private const val DATABASE_NAME = "vakildiary.db"
        private const val DOCUMENTS_DIR = "documents"
        private const val DATASTORE_NAME = "user_prefs"
    }
}
