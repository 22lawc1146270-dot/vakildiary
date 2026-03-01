package com.vakildiary.app.data.repository

import com.vakildiary.app.core.Result
import com.google.gson.Gson
import com.vakildiary.app.data.local.dao.JudgmentMetadataDao
import com.vakildiary.app.data.local.entities.JudgmentMetadataEntity
import android.util.Log
import com.vakildiary.app.data.remote.judgments.JudgmentMetadataDto
import com.vakildiary.app.data.remote.judgments.MetadataIndexPartDto
import com.vakildiary.app.data.remote.judgments.SCJudgmentService
import com.vakildiary.app.domain.repository.JudgmentDownload
import com.vakildiary.app.domain.repository.JudgmentSearchResult
import com.vakildiary.app.domain.repository.SCJudgmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream
import javax.inject.Inject

class SCJudgmentRepositoryImpl @Inject constructor(
    private val service: SCJudgmentService,
    private val judgmentMetadataDao: JudgmentMetadataDao
) : SCJudgmentRepository {
    private val gson = Gson()

    override suspend fun searchJudgments(
        query: String,
        year: String
    ): Result<List<JudgmentSearchResult>> {
        return try {
            val yearInt = year.toIntOrNull() ?: return Result.Error("Please enter a valid year")
            ensureMetadataCached(yearInt)
            val normalizedQuery = query.trim().lowercase()
            val entities = if (normalizedQuery.isBlank()) {
                judgmentMetadataDao.getByYear(yearInt)
            } else {
                judgmentMetadataDao.searchByYear(yearInt, normalizedQuery)
            }
            Result.Success(entities.map { it.toSearchResult() })
        } catch (t: Throwable) {
            Result.Error("Judgment search failed", t)
        }
    }

    override suspend fun downloadJudgment(
        judgmentId: String
    ): Result<JudgmentDownload> {
        return withContext(Dispatchers.IO) {
            try {
            val entity = judgmentMetadataDao.getById(judgmentId)
            if (entity == null) {
                return@withContext Result.Error("Judgment not found in cache")
            }
            val archivePath = "data/tar/year=${entity.year}/english/${entity.archiveName}"
            Log.d(TAG, "Downloading judgmentId=$judgmentId archive=$archivePath target=${entity.fileName}")
            val pdfBytes = service.downloadArchive(archivePath).use { body ->
                extractPdfFromTar(body.byteStream(), entity.fileName)
            } ?: return@withContext Result.Error("Judgment PDF not found in archive")
            val mimeType = "application/pdf"
            Result.Success(
                JudgmentDownload(
                    fileName = entity.fileName,
                    mimeType = mimeType,
                    inputStream = pdfBytes.inputStream()
                )
            )
            } catch (t: Throwable) {
                Log.e(TAG, "Failed to download judgmentId=$judgmentId", t)
                val message = when (t) {
                    is HttpException -> "Failed to download judgment (HTTP ${t.code()})"
                    is IOException -> "Network error while downloading judgment"
                    else -> "Failed to download judgment"
                }
                Result.Error(message, t)
            }
        }
    }

    private suspend fun ensureMetadataCached(year: Int) = withContext(Dispatchers.IO) {
        if (judgmentMetadataDao.countByYear(year) > 0) return@withContext
        judgmentMetadataDao.deleteByYear(year)
        val yearString = year.toString()
        val englishIndex = try {
            service.getEnglishIndex(yearString)
        } catch (t: Throwable) {
            if (t is HttpException && t.code() == 404) return@withContext
            throw t
        }
        val metadataIndex = try {
            service.getMetadataIndex(yearString)
        } catch (t: Throwable) {
            if (t is HttpException && t.code() == 404) null else throw t
        }
        val archiveByFile = buildArchiveLookup(englishIndex.parts)
        val indexedFiles = archiveByFile.entries.associate { entry ->
            entry.key.lowercase() to IndexedArchiveFile(
                fileName = entry.key,
                archiveName = entry.value
            )
        }
        val syncedAt = System.currentTimeMillis()
        val entitiesById = linkedMapOf<String, JudgmentMetadataEntity>()

        metadataIndex?.parts?.forEach { part ->
            val partName = part.partName() ?: return@forEach
            val partPath = "data/zip/year=$yearString/$partName"
            runCatching {
                service.downloadArchive(partPath).use { body ->
                    val payload = readMetadataPayload(partName, body)
                    parseMetadataPayload(payload)
                }
            }.onSuccess { entries ->
                entries.forEach { dto ->
                    val indexed = resolveIndexedArchiveFile(dto, indexedFiles) ?: return@forEach
                    val parsed = dto.rawHtml
                        ?.takeIf { it.isNotBlank() }
                        ?.let(::parseMetadata)
                        ?: ParsedMetadata.empty()
                    val petitioner = dto.petitioner?.ifBlank { null } ?: parsed.petitioner
                    val respondent = dto.respondent?.ifBlank { null } ?: parsed.respondent
                    val caseId = dto.caseId?.ifBlank { null }
                        ?: parsed.caseNumber
                        ?: extractCaseIdFromFileName(indexed.fileName)
                    val citation = dto.citationDisplay?.ifBlank { null }
                        ?: dto.citation?.ifBlank { null }
                    val decisionDate = parseDecisionDateValue(dto.decisionDate) ?: parsed.decisionDate
                    val title = buildTitle(
                        petitioner = petitioner,
                        respondent = respondent,
                        citation = citation,
                        caseId = caseId,
                        fallbackFileName = indexed.fileName
                    )
                    val searchText = buildSearchText(
                        petitioner = petitioner,
                        respondent = respondent,
                        caseId = caseId,
                        citation = citation,
                        title = title,
                        coram = parsed.coram,
                        bench = parsed.bench,
                        rawText = parsed.rawText
                    )
                    entitiesById[indexed.fileName] = JudgmentMetadataEntity(
                        judgmentId = indexed.fileName,
                        title = title,
                        citation = citation,
                        bench = parsed.bench,
                        coram = parsed.coram,
                        caseNumber = caseId,
                        petitioner = petitioner,
                        respondent = respondent,
                        year = year,
                        archiveName = indexed.archiveName,
                        fileName = indexed.fileName,
                        dateOfJudgment = decisionDate,
                        searchText = searchText,
                        syncedAt = syncedAt
                    )
                }
            }.onFailure { error ->
                Log.w(TAG, "Failed parsing metadata part: $partPath", error)
            }
        }

        archiveByFile.forEach { (fileName, archiveName) ->
            if (entitiesById.containsKey(fileName)) return@forEach
            entitiesById[fileName] = createFallbackEntity(
                fileName = fileName,
                archiveName = archiveName,
                year = year,
                syncedAt = syncedAt
            )
        }

        if (entitiesById.isNotEmpty()) {
            judgmentMetadataDao.upsertAll(entitiesById.values.toList())
        }
    }

    private fun extractCaseIdFromFileName(fileName: String): String? {
        return fileName
            .removeSuffix(".pdf")
            .removeSuffix("_EN")
            .trim()
            .takeIf { it.isNotBlank() }
    }

    private fun resolveIndexedArchiveFile(
        dto: JudgmentMetadataDto,
        indexedFiles: Map<String, IndexedArchiveFile>
    ): IndexedArchiveFile? {
        val candidates = buildFileNameCandidates(dto)
        candidates.forEach { candidate ->
            indexedFiles[candidate.lowercase()]?.let { return it }
        }
        val normalizedCaseId = dto.caseId
            ?.replace(Regex("[^A-Za-z0-9]+"), "_")
            ?.trim('_')
            ?.lowercase()
            ?: return null
        return indexedFiles.entries
            .firstOrNull { (fileName, _) -> fileName.contains(normalizedCaseId) }
            ?.value
    }

    private fun buildFileNameCandidates(dto: JudgmentMetadataDto): Set<String> {
        val candidates = linkedSetOf<String>()
        val path = dto.path?.trim().orEmpty()
        if (path.isNotBlank()) {
            val normalizedPath = path.replace('\\', '/')
            val baseName = normalizedPath.substringAfterLast('/')
            addFileCandidates(baseName, candidates)
            addFileCandidates("${baseName}_EN", candidates)
        }
        val caseId = dto.caseId?.trim().orEmpty()
        if (caseId.isNotBlank()) {
            addFileCandidates(caseId, candidates)
            addFileCandidates(caseId.replace(Regex("\\s+"), "_"), candidates)
            addFileCandidates(caseId.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_'), candidates)
        }
        return candidates
    }

    private fun addFileCandidates(value: String, target: MutableSet<String>) {
        val cleaned = value.trim().replace('\\', '/').substringAfterLast('/').trim()
        if (cleaned.isBlank()) return
        if (cleaned.endsWith(".pdf", ignoreCase = true)) {
            target += cleaned
            return
        }
        target += "$cleaned.pdf"
        target += "${cleaned}_EN.pdf"
    }

    private fun createFallbackEntity(
        fileName: String,
        archiveName: String,
        year: Int,
        syncedAt: Long
    ): JudgmentMetadataEntity {
        val caseId = extractCaseIdFromFileName(fileName)
        val title = fileName.removeSuffix(".pdf").replace('_', ' ')
        return JudgmentMetadataEntity(
            judgmentId = fileName,
            title = title,
            citation = null,
            bench = null,
            coram = null,
            caseNumber = caseId,
            petitioner = null,
            respondent = null,
            year = year,
            archiveName = archiveName,
            fileName = fileName,
            dateOfJudgment = null,
            searchText = buildSearchText(
                petitioner = null,
                respondent = null,
                caseId = caseId,
                citation = title,
                title = title,
                coram = null,
                bench = null,
                rawText = null
            ),
            syncedAt = syncedAt
        )
    }

    private fun buildTitle(
        petitioner: String?,
        respondent: String?,
        citation: String?,
        caseId: String?,
        fallbackFileName: String
    ): String {
        val p = petitioner?.trim().orEmpty()
        val r = respondent?.trim().orEmpty()
        if (p.isNotBlank() && r.isNotBlank()) {
            val citationPart = citation?.trim().orEmpty()
            return if (citationPart.isNotBlank()) {
                "$p v. $r ($citationPart)"
            } else {
                "$p v. $r"
            }
        }
        return caseId?.takeIf { it.isNotBlank() }
            ?: citation?.takeIf { it.isNotBlank() }
            ?: fallbackFileName.removeSuffix(".pdf").replace('_', ' ')
    }

    private fun buildArchiveLookup(parts: List<MetadataIndexPartDto>): Map<String, String> {
        val lookup = mutableMapOf<String, String>()
        parts.forEach { part ->
            val archiveName = part.partName() ?: return@forEach
            part.files.forEach { file ->
                lookup[file] = archiveName
            }
        }
        return lookup
    }

    private fun readEntry(input: TarArchiveInputStream): String {
        val output = ByteArrayOutputStream()
        input.copyTo(output)
        return output.toString(Charsets.UTF_8.name())
    }

    private fun extractPdfFromTar(stream: java.io.InputStream, targetFile: String): ByteArray? {
        val normalizedTarget = targetFile.replace('\\', '/')
        val targetBaseName = normalizedTarget.substringAfterLast('/')
        TarArchiveInputStream(stream).use { tarInput ->
            while (true) {
                val entry = tarInput.nextTarEntry ?: break
                if (!entry.isFile) continue
                val normalizedEntry = entry.name.replace('\\', '/')
                val entryName = normalizedEntry.substringAfterLast('/')
                val matches = normalizedEntry.equals(normalizedTarget, ignoreCase = true) ||
                    entryName.equals(targetBaseName, ignoreCase = true)
                if (!matches) continue
                val output = ByteArrayOutputStream()
                tarInput.copyTo(output)
                return output.toByteArray()
            }
        }
        Log.w(TAG, "PDF not found in archive for target=$targetFile")
        return null
    }

    private fun buildSearchText(
        petitioner: String?,
        respondent: String?,
        caseId: String?,
        citation: String?,
        title: String?,
        coram: String?,
        bench: String?,
        rawText: String?
    ): String {
        return listOfNotNull(petitioner, respondent, caseId, citation, title, coram, bench, rawText)
            .joinToString(" ")
            .lowercase()
    }

    private fun parseMetadata(rawHtml: String): ParsedMetadata {
        val decisionDate = parseDecisionDate(rawHtml)
        val caseNumber = extractValue(rawHtml, "Case No")
        val bench = extractValue(rawHtml, "Bench")
        val coram = parseCoram(rawHtml)
        val petitioner = extractValue(rawHtml, "Petitioner") ?: extractValue(rawHtml, "Appellant")
        val respondent = extractValue(rawHtml, "Respondent")
        val rawText = stripHtml(rawHtml)
        return ParsedMetadata(
            decisionDate = decisionDate,
            caseNumber = caseNumber,
            bench = bench,
            coram = coram,
            petitioner = petitioner,
            respondent = respondent,
            rawText = rawText
        )
    }

    private fun extractValue(rawHtml: String, label: String): String? {
        val regex = Regex("$label\\s*:\\s*</span>\\s*<font[^>]*>\\s*([^<]+)", RegexOption.IGNORE_CASE)
        val match = regex.find(rawHtml)?.groupValues?.getOrNull(1)?.trim()
        return match?.takeIf { it.isNotBlank() }
    }

    private fun parseDecisionDate(rawHtml: String): Long? {
        val value = extractValue(rawHtml, "Decision Date") ?: return null
        return parseDateParts(value)
    }

    private fun parseDecisionDateValue(value: String?): Long? {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isBlank()) return null
        val numeric = trimmed.toLongOrNull()
        if (numeric != null) {
            return if (numeric < 100_000_000_000L) numeric * 1000 else numeric
        }
        val instant = runCatching { java.time.Instant.parse(trimmed) }.getOrNull()
        if (instant != null) return instant.toEpochMilli()
        val localDate = runCatching { java.time.LocalDate.parse(trimmed) }.getOrNull()
        if (localDate != null) {
            return localDate.atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        }
        return parseDateParts(trimmed)
    }

    private fun parseDateParts(value: String): Long? {
        val parts = value.split("-", "/", ".").map { it.trim() }.filter { it.isNotBlank() }
        if (parts.size != 3) return null
        val first = parts[0].toIntOrNull() ?: return null
        val second = parts[1].toIntOrNull() ?: return null
        val third = parts[2].toIntOrNull() ?: return null
        val (year, month, day) = if (parts[0].length == 4) {
            Triple(first, second, third)
        } else {
            Triple(third, second, first)
        }
        return runCatching {
            java.time.LocalDate.of(year, month, day)
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        }.getOrNull()
    }

    private fun parseCoram(rawHtml: String): String? {
        val regex = Regex("Coram\\s*:\\s*(.*?)</strong>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val match = regex.find(rawHtml)?.groupValues?.getOrNull(1) ?: return null
        val cleaned = stripHtml(match)
        return cleaned.takeIf { it.isNotBlank() }
    }

    private fun stripHtml(rawHtml: String): String {
        return rawHtml
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("\\s+".toRegex(), " ")
            .trim()
    }

    private fun readMetadataPayload(partName: String, body: okhttp3.ResponseBody): String {
        val stream = body.byteStream()
        return when {
            partName.endsWith(".gz", ignoreCase = true) -> {
                GZIPInputStream(stream).use { it.readBytes().toString(Charsets.UTF_8) }
            }
            partName.endsWith(".zip", ignoreCase = true) -> {
                ZipInputStream(stream).use { zip ->
                    val entry = zip.nextEntry ?: return ""
                    val payload = zip.readBytes().toString(Charsets.UTF_8)
                    zip.closeEntry()
                    payload
                }
            }
            else -> stream.readBytes().toString(Charsets.UTF_8)
        }
    }

    private fun parseMetadataPayload(payload: String): List<JudgmentMetadataDto> {
        val trimmed = payload.trim()
        if (trimmed.isBlank()) return emptyList()
        if (trimmed.startsWith("[")) {
            return gson.fromJson(trimmed, Array<JudgmentMetadataDto>::class.java).toList()
        }
        return trimmed.lineSequence()
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { line ->
                runCatching { gson.fromJson(line, JudgmentMetadataDto::class.java) }.getOrNull()
            }
            .toList()
    }

    private fun JudgmentMetadataEntity.toSearchResult(): JudgmentSearchResult {
        return JudgmentSearchResult(
            judgmentId = judgmentId,
            title = title,
            citation = citation,
            bench = bench,
            coram = coram,
            caseNumber = caseNumber,
            petitioner = petitioner,
            respondent = respondent,
            dateOfJudgment = dateOfJudgment
        )
    }

    private data class ParsedMetadata(
        val decisionDate: Long?,
        val caseNumber: String?,
        val bench: String?,
        val coram: String?,
        val petitioner: String?,
        val respondent: String?,
        val rawText: String
    ) {
        companion object {
            fun empty() = ParsedMetadata(
                decisionDate = null,
                caseNumber = null,
                bench = null,
                coram = null,
                petitioner = null,
                respondent = null,
                rawText = ""
            )
        }
    }

    private data class IndexedArchiveFile(
        val fileName: String,
        val archiveName: String
    )

    companion object {
        private const val TAG = "SCJudgmentRepo"
    }
}
