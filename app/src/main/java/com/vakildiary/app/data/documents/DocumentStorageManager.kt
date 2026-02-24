package com.vakildiary.app.data.documents

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import android.webkit.MimeTypeMap
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.storage.DocumentStorage
import com.vakildiary.app.domain.storage.StoredDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.UUID
import kotlin.io.DEFAULT_BUFFER_SIZE
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DocumentStorageManager @Inject constructor(
    @ApplicationContext private val context: Context
) : DocumentStorage {

    private val masterKey: MasterKey by lazy {
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
    }

    override suspend fun saveDocument(
        caseId: String?,
        fileName: String,
        mimeType: String,
        inputStreamProvider: () -> InputStream,
        isScanned: Boolean,
        tags: String
    ): Result<StoredDocument> = withContext(Dispatchers.IO) {
        return@withContext try {
            val sanitizedName = sanitizeFileName(fileName)
            val extension = getExtension(sanitizedName, mimeType)
            val finalName = ensureUniqueFileName(caseId, sanitizedName, extension)
            val targetFile = File(getCaseDir(caseId), finalName)

            inputStreamProvider().use { input ->
                val encryptedFile = EncryptedFile.Builder(
                    context,
                    targetFile,
                    masterKey,
                    EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
                ).build()
                encryptedFile.openFileOutput().use { output ->
                    input.copyTo(output, bufferSize = DEFAULT_BUFFER_SIZE)
                }
            }

            Result.Success(
                StoredDocument(
                    fileName = finalName,
                    filePath = targetFile.absolutePath,
                    fileType = mimeType,
                    fileSizeBytes = targetFile.length(),
                    isScanned = isScanned,
                    thumbnailPath = null,
                    tags = tags
                )
            )
        } catch (t: Throwable) {
            Result.Error("Failed to save document", t)
        }
    }

    override suspend fun renameDocument(filePath: String, newFileName: String): Result<String> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val source = File(filePath)
                if (!source.exists()) return@withContext Result.Error("File not found")
                val target = File(source.parentFile, sanitizeFileName(newFileName))
                if (source.renameTo(target)) {
                    Result.Success(target.absolutePath)
                } else {
                    Result.Error("Failed to rename file")
                }
            } catch (t: Throwable) {
                Result.Error("Failed to rename file", t)
            }
        }

    override suspend fun moveDocument(filePath: String, newCaseId: String?): Result<String> =
        withContext(Dispatchers.IO) {
            return@withContext try {
                val source = File(filePath)
                if (!source.exists()) return@withContext Result.Error("File not found")
                val targetDir = getCaseDir(newCaseId)
                val target = File(targetDir, source.name)
                if (source.renameTo(target)) {
                    Result.Success(target.absolutePath)
                } else {
                    Result.Error("Failed to move file")
                }
            } catch (t: Throwable) {
                Result.Error("Failed to move file", t)
            }
        }

    override suspend fun deleteFile(filePath: String): Result<Unit> = withContext(Dispatchers.IO) {
        return@withContext try {
            val file = File(filePath)
            if (!file.exists()) return@withContext Result.Success(Unit)
            if (file.delete()) Result.Success(Unit) else Result.Error("Failed to delete file")
        } catch (t: Throwable) {
            Result.Error("Failed to delete file", t)
        }
    }

    override suspend fun prepareFileForViewing(filePath: String): Result<File> = withContext(Dispatchers.IO) {
        return@withContext try {
            val source = File(filePath)
            if (!source.exists()) return@withContext Result.Error("File not found")
            val cacheDir = File(context.cacheDir, "document_cache")
            if (!cacheDir.exists()) cacheDir.mkdirs()
            val target = File(cacheDir, source.name)

            val encryptedFile = EncryptedFile.Builder(
                context,
                source,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            try {
                encryptedFile.openFileInput().use { input ->
                    FileOutputStream(target).use { output ->
                        input.copyTo(output, bufferSize = DEFAULT_BUFFER_SIZE)
                    }
                }
            } catch (t: Throwable) {
                // Fallback for legacy unencrypted files.
                source.inputStream().use { input ->
                    FileOutputStream(target).use { output ->
                        input.copyTo(output, bufferSize = DEFAULT_BUFFER_SIZE)
                    }
                }
            }

            Result.Success(target)
        } catch (t: Throwable) {
            Result.Error("Failed to prepare file", t)
        }
    }

    private fun getCaseDir(caseId: String?): File {
        val base = File(context.filesDir, DOCUMENTS_DIR)
        val dir = if (caseId.isNullOrBlank()) File(base, "general") else File(base, caseId)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun sanitizeFileName(name: String): String {
        return name.replace(Regex("[^A-Za-z0-9._\\- ]"), "_")
    }

    private fun getExtension(fileName: String, mimeType: String): String {
        if (fileName.contains('.')) return fileName.substringAfterLast('.')
        val ext = MimeTypeMap.getSingleton().getExtensionFromMimeType(mimeType)
        return ext ?: "bin"
    }

    private fun ensureUniqueFileName(caseId: String?, baseName: String, extension: String): String {
        val nameWithoutExt = baseName.substringBeforeLast('.')
        var candidate = "$nameWithoutExt.$extension"
        val dir = getCaseDir(caseId)
        if (!File(dir, candidate).exists()) return candidate
        val suffix = UUID.randomUUID().toString().take(8)
        candidate = "$nameWithoutExt-$suffix.$extension"
        return candidate
    }

    companion object {
        private const val DOCUMENTS_DIR = "documents"
    }
}
