package com.vakildiary.app.data.repository

import com.vakildiary.app.core.Result
import com.google.gson.Gson
import com.vakildiary.app.data.local.dao.JudgmentMetadataDao
import com.vakildiary.app.data.local.entities.JudgmentMetadataEntity
import com.vakildiary.app.data.remote.judgments.JudgmentMetadataDto
import com.vakildiary.app.data.remote.judgments.MetadataIndexPartDto
import com.vakildiary.app.data.remote.judgments.SCJudgmentService
import com.vakildiary.app.domain.repository.JudgmentDownload
import com.vakildiary.app.domain.repository.JudgmentSearchResult
import com.vakildiary.app.domain.repository.SCJudgmentRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import java.io.ByteArrayOutputStream
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
        return try {
            val entity = judgmentMetadataDao.getById(judgmentId)
                ?: return Result.Error("Judgment not found in cache")
            val archivePath = "data/tar/year=${entity.year}/english/${entity.archiveName}"
            val pdfBytes = service.downloadArchive(archivePath).use { body ->
                extractPdfFromTar(body.byteStream(), entity.fileName)
            } ?: return Result.Error("Judgment PDF not found in archive")
            val mimeType = "application/pdf"
            Result.Success(
                JudgmentDownload(
                    fileName = entity.fileName,
                    mimeType = mimeType,
                    inputStream = pdfBytes.inputStream()
                )
            )
        } catch (t: Throwable) {
            Result.Error("Failed to download judgment", t)
        }
    }

    private suspend fun ensureMetadataCached(year: Int) = withContext(Dispatchers.IO) {
        if (judgmentMetadataDao.countByYear(year) > 0) return@withContext
        judgmentMetadataDao.deleteByYear(year)
        val yearString = year.toString()
        val metadataIndex = service.getMetadataIndex(yearString)
        val englishIndex = service.getEnglishIndex(yearString)
        val fileToArchive = buildArchiveLookup(englishIndex.parts)
        val syncedAt = System.currentTimeMillis()
        metadataIndex.parts.forEach { part ->
            val archiveName = part.partName() ?: return@forEach
            val archivePath = "metadata/tar/year=$yearString/$archiveName"
            val entities = mutableListOf<JudgmentMetadataEntity>()
            service.downloadArchive(archivePath).use { body ->
                body.byteStream().use { stream ->
                    TarArchiveInputStream(stream).use { tarInput ->
                        while (true) {
                            val entry = tarInput.nextTarEntry ?: break
                            if (!entry.isFile || !entry.name.endsWith(".json")) continue
                            val json = readEntry(tarInput)
                            val dto = gson.fromJson(json, JudgmentMetadataDto::class.java)
                            val path = dto.path?.trim().orEmpty()
                            if (path.isBlank()) continue
                            val fileName = "${path}_EN.pdf"
                            val fileArchive = fileToArchive[fileName] ?: continue
                            val parsed = parseMetadata(dto.rawHtml.orEmpty())
                            val title = parsed.caseNumber?.ifBlank { null }
                                ?: dto.citationDisplay?.ifBlank { null }
                                ?: fileName.removeSuffix(".pdf").replace('_', ' ')
                            val searchText = buildSearchText(dto, parsed)
                            entities.add(
                                JudgmentMetadataEntity(
                                    judgmentId = fileName,
                                    title = title,
                                    citation = dto.citationDisplay,
                                    bench = parsed.bench,
                                    coram = parsed.coram,
                                    caseNumber = parsed.caseNumber,
                                    year = year,
                                    archiveName = fileArchive,
                                    fileName = fileName,
                                    dateOfJudgment = parsed.decisionDate,
                                    searchText = searchText,
                                    syncedAt = syncedAt
                                )
                            )
                        }
                    }
                }
            }
            if (entities.isNotEmpty()) {
                judgmentMetadataDao.upsertAll(entities)
            }
        }
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
        TarArchiveInputStream(stream).use { tarInput ->
            while (true) {
                val entry = tarInput.nextTarEntry ?: break
                if (!entry.isFile) continue
                val entryName = entry.name.substringAfterLast('/')
                if (!entryName.equals(targetFile, ignoreCase = true)) continue
                val output = ByteArrayOutputStream()
                tarInput.copyTo(output)
                return output.toByteArray()
            }
        }
        return null
    }

    private fun buildSearchText(dto: JudgmentMetadataDto, parsed: ParsedMetadata): String {
        val raw = parsed.rawText
        val tokens = listOfNotNull(
            parsed.caseNumber,
            parsed.bench,
            parsed.coram,
            dto.citationDisplay
        ).joinToString(" ")
        return "$raw $tokens".lowercase()
    }

    private fun parseMetadata(rawHtml: String): ParsedMetadata {
        val decisionDate = parseDecisionDate(rawHtml)
        val caseNumber = extractValue(rawHtml, "Case No")
        val bench = extractValue(rawHtml, "Bench")
        val coram = parseCoram(rawHtml)
        val rawText = stripHtml(rawHtml)
        return ParsedMetadata(
            decisionDate = decisionDate,
            caseNumber = caseNumber,
            bench = bench,
            coram = coram,
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
        val parts = value.split("-", "/", ".").map { it.trim() }.filter { it.isNotBlank() }
        if (parts.size != 3) return null
        return runCatching {
            val day = parts[0].toInt()
            val month = parts[1].toInt()
            val year = parts[2].toInt()
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

    private fun JudgmentMetadataEntity.toSearchResult(): JudgmentSearchResult {
        return JudgmentSearchResult(
            judgmentId = judgmentId,
            title = title,
            citation = citation,
            bench = bench,
            coram = coram,
            caseNumber = caseNumber,
            dateOfJudgment = dateOfJudgment
        )
    }

    private data class ParsedMetadata(
        val decisionDate: Long?,
        val caseNumber: String?,
        val bench: String?,
        val coram: String?,
        val rawText: String
    )
}
