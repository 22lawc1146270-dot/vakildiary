package com.vakildiary.app.data.backup

import com.google.api.client.http.FileContent
import com.google.api.client.http.HttpRequestInitializer
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.vakildiary.app.core.Result
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import java.io.File as IoFile

@Singleton
class DriveBackupManager @Inject constructor() {
    private var drive: Drive? = null

    fun initialize(requestInitializer: HttpRequestInitializer, applicationName: String) {
        val transport = NetHttpTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()
        drive = Drive.Builder(transport, jsonFactory, requestInitializer)
            .setApplicationName(applicationName)
            .build()
    }

    suspend fun uploadBackup(
        file: IoFile,
        mimeType: String,
        folderId: String? = null
    ): Result<String> = withContext(Dispatchers.IO) {
        val service = drive ?: return@withContext Result.Error("Drive not initialized")
        return@withContext try {
            val metadata = File().apply {
                name = file.name
                folderId?.let { parents = listOf(it) }
            }
            val mediaContent = FileContent(mimeType, file)
            val result = service.files().create(metadata, mediaContent)
                .setFields("id")
                .execute()
            Result.Success(result.id)
        } catch (t: Throwable) {
            Result.Error("Drive upload failed", t)
        }
    }

    suspend fun downloadBackup(fileId: String, destination: IoFile): Result<Unit> =
        withContext(Dispatchers.IO) {
            val service = drive ?: return@withContext Result.Error("Drive not initialized")
            return@withContext try {
                destination.outputStream().use { output ->
                    service.files().get(fileId).executeMediaAndDownloadTo(output)
                }
                Result.Success(Unit)
            } catch (t: Throwable) {
                Result.Error("Drive download failed", t)
            }
        }

    suspend fun listBackups(query: String): Result<List<File>> = withContext(Dispatchers.IO) {
        val service = drive ?: return@withContext Result.Error("Drive not initialized")
        return@withContext try {
            val result = service.files().list()
                .setQ(query)
                .setFields("files(id,name,modifiedTime,size)")
                .execute()
            Result.Success(result.files ?: emptyList())
        } catch (t: Throwable) {
            Result.Error("Drive list failed", t)
        }
    }
}
