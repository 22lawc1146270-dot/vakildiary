package com.vakildiary.app.data.repository

import com.vakildiary.app.core.Result
import com.vakildiary.app.data.remote.judgments.SCJudgmentService
import com.vakildiary.app.domain.repository.JudgmentDownload
import com.vakildiary.app.domain.repository.JudgmentSearchResult
import com.vakildiary.app.domain.repository.SCJudgmentRepository
import javax.inject.Inject

class SCJudgmentRepositoryImpl @Inject constructor(
    private val service: SCJudgmentService
) : SCJudgmentRepository {

    override suspend fun searchJudgments(
        query: String,
        year: String
    ): Result<List<JudgmentSearchResult>> {
        return try {
            val index = service.getMetadataIndex(year)
            val allFiles = index.parts.flatMap { it.files }
            val results = allFiles.map { buildResult(it) }
            val filtered = if (query.isBlank()) {
                results
            } else {
                results.filter { matchesQuery(it, query) }
            }
            Result.Success(filtered)
        } catch (t: Throwable) {
            Result.Error("Judgment search failed", t)
        }
    }

    override suspend fun downloadJudgment(
        judgmentId: String
    ): Result<JudgmentDownload> {
        return try {
            val body = service.downloadJudgment(judgmentId)
            val fileName = buildFileName(judgmentId)
            val mimeType = body.contentType()?.toString() ?: "application/pdf"
            Result.Success(
                JudgmentDownload(
                    fileName = fileName,
                    mimeType = mimeType,
                    inputStream = body.byteStream()
                )
            )
        } catch (t: Throwable) {
            Result.Error("Failed to download judgment", t)
        }
    }

    private fun buildFileName(judgmentId: String): String {
        val rawName = judgmentId.substringAfterLast('/').ifBlank { "judgment_${System.currentTimeMillis()}.pdf" }
        return if (rawName.contains('.')) rawName else "$rawName.pdf"
    }

    private fun buildResult(fileName: String): JudgmentSearchResult {
        val cleanName = fileName.substringAfterLast('/')
        val title = cleanName.substringBeforeLast('.')
            .replace('_', ' ')
            .replace('-', ' ')
            .trim()
            .ifBlank { cleanName }
        return JudgmentSearchResult(
            judgmentId = fileName,
            title = title,
            citation = parseCitation(cleanName),
            bench = parseBench(cleanName),
            dateOfJudgment = parseDate(cleanName)
        )
    }

    private fun parseCitation(name: String): String? {
        val normalized = name.replace('_', ' ').replace('-', ' ')
        val scc = Regex("\\b\\d{4}\\s*SCC\\s*\\d+\\b", RegexOption.IGNORE_CASE).find(normalized)
        if (scc != null) return scc.value.replace(Regex("\\s+"), " ").trim()
        val air = Regex("\\bAIR\\s*\\d{4}\\s*SC\\s*\\d+\\b", RegexOption.IGNORE_CASE).find(normalized)
        return air?.value?.replace(Regex("\\s+"), " ")?.trim()
    }

    private fun parseBench(name: String): String? {
        val normalized = name.replace('_', ' ').replace('-', ' ')
        return when {
            normalized.contains("constitution bench", ignoreCase = true) -> "Constitution Bench"
            normalized.contains("bench", ignoreCase = true) -> "Bench"
            else -> null
        }
    }

    private fun parseDate(name: String): Long? {
        val normalized = name.replace('_', '-').replace(' ', '-')
        val iso = Regex("\\d{4}-\\d{2}-\\d{2}").find(normalized)?.value
        if (iso != null) return parseIsoDate(iso)
        val ddmmyyyy = Regex("\\d{2}-\\d{2}-\\d{4}").find(normalized)?.value
        if (ddmmyyyy != null) return parseSlashDate(ddmmyyyy.replace('-', '/'))
        val compact = Regex("\\d{8}").find(normalized)?.value
        if (compact != null) {
            val year = compact.substring(0, 4)
            val month = compact.substring(4, 6)
            val day = compact.substring(6, 8)
            return parseIsoDate("$year-$month-$day")
        }
        return null
    }

    private fun parseIsoDate(value: String): Long? {
        return runCatching {
            java.time.LocalDate.parse(value)
                .atStartOfDay(java.time.ZoneId.systemDefault())
                .toInstant().toEpochMilli()
        }.getOrNull()
    }

    private fun parseSlashDate(value: String): Long? {
        val parts = value.split("/")
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

    private fun matchesQuery(result: JudgmentSearchResult, query: String): Boolean {
        val normalized = query.trim().lowercase()
        return result.title.lowercase().contains(normalized) ||
            result.judgmentId.lowercase().contains(normalized) ||
            (result.citation?.lowercase()?.contains(normalized) == true) ||
            (result.bench?.lowercase()?.contains(normalized) == true)
    }
}
