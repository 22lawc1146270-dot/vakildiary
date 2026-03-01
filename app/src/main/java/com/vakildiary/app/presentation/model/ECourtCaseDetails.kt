package com.vakildiary.app.presentation.model

import com.vakildiary.app.domain.model.CourtType

data class ECourtCaseDetails(
    val caseTitle: String,
    val caseNumber: String,
    val courtName: String,
    val courtType: CourtType?,
    val parties: String,
    val stage: String?,
    val nextHearingDate: String?,
    val caseDetails: List<String>,
    val caseStatus: List<String>,
    val petitionerAdvocate: List<String>,
    val respondentAdvocate: List<String>,
    val acts: List<String>,
    val caseHistory: List<String>,
    val transferDetails: List<String>
)

object ECourtDetailParser {
    fun parse(raw: String, item: ECourtCaseItem): ECourtCaseDetails {
        val heading = extractById(raw, "chHeading")
        val caseDetails = extractTableByClass(raw, "case_details_table")
            .ifEmpty { extractSectionByTitles(raw, listOf("Case Details")) }
        val caseStatus = extractTableByClass(raw, "case_status_table")
            .ifEmpty { extractSectionByTitles(raw, listOf("Case Status")) }
        val petitionerAdvocate = extractListByClass(raw, "petitioner-advocate-list")
            .ifEmpty { extractSectionByTitles(raw, listOf("Petitioner and Advocate")) }
        val respondentAdvocate = extractListByClass(raw, "respondent-advocate-list")
            .ifEmpty { extractSectionByTitles(raw, listOf("Respondent and Advocate")) }
        val caseHistory = extractTableByClass(raw, "history_table")
            .ifEmpty { extractSectionByTitles(raw, listOf("Case History")) }
        val transferDetails = extractTableByClass(raw, "transfer_table")
            .ifEmpty {
                extractSectionByTitles(
                    raw,
                    listOf("Case Transfer Details within Establishment", "Transfer Details")
                )
            }
        val acts = extractSectionByTitles(raw, listOf("Acts"))
        return ECourtCaseDetails(
            caseTitle = heading.ifBlank { item.caseTitle },
            caseNumber = item.caseNumber,
            courtName = item.courtName,
            courtType = item.courtType,
            parties = item.parties,
            stage = item.stage.takeIf { it.isNotBlank() },
            nextHearingDate = item.nextHearingDate.takeIf { it.isNotBlank() },
            caseDetails = caseDetails,
            caseStatus = caseStatus,
            petitionerAdvocate = petitionerAdvocate,
            respondentAdvocate = respondentAdvocate,
            acts = acts,
            caseHistory = caseHistory,
            transferDetails = transferDetails
        )
    }

    private fun extractSectionByTitles(raw: String, titles: List<String>): List<String> {
        titles.forEach { title ->
            val lines = extractSection(raw, title)
            if (lines.isNotEmpty()) return lines
        }
        return emptyList()
    }

    private fun extractSection(raw: String, title: String): List<String> {
        val normalized = raw.replace("&nbsp;", " ").replace("&amp;", "&")
        val headingRegex = Regex(title, RegexOption.IGNORE_CASE)
        val headingMatch = headingRegex.find(normalized) ?: return emptyList()
        val afterHeading = normalized.substring(headingMatch.range.last + 1)
        val tableMatch = Regex(
            "<table[^>]*>.*?</table>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(afterHeading) ?: return emptyList()
        return tableToLines(tableMatch.value)
    }

    private fun extractById(raw: String, id: String): String {
        val match = Regex(
            "<[^>]*id=['\"]$id['\"][^>]*>(.*?)</[^>]+>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(raw) ?: return ""
        return clean(match.groupValues[1])
    }

    private fun extractTableByClass(raw: String, classToken: String): List<String> {
        val match = Regex(
            "<table[^>]*class=['\"][^'\"]*\\b${Regex.escape(classToken)}\\b[^'\"]*['\"][^>]*>.*?</table>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(raw) ?: return emptyList()
        return tableToLines(match.value)
    }

    private fun extractListByClass(raw: String, classToken: String): List<String> {
        val match = Regex(
            "<ul[^>]*class=['\"][^'\"]*\\b${Regex.escape(classToken)}\\b[^'\"]*['\"][^>]*>(.*?)</ul>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(raw) ?: return emptyList()
        val body = match.groupValues[1]
        val items = Regex("<li[^>]*>(.*?)</li>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
            .findAll(body)
            .map { clean(it.groupValues[1]) }
            .filter { it.isNotBlank() }
            .toList()
        if (items.isNotEmpty()) return items
        return body.split(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE))
            .map { clean(it) }
            .filter { it.isNotBlank() }
    }

    private fun tableToLines(tableHtml: String): List<String> {
        val rowRegex = Regex("<tr[^>]*>(.*?)</tr>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val cellRegex = Regex("<t[dh][^>]*>(.*?)</t[dh]>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
        val lines = mutableListOf<String>()
        rowRegex.findAll(tableHtml).forEach { rowMatch ->
            val row = rowMatch.groupValues[1]
            val cells = cellRegex.findAll(row).map { clean(it.groupValues[1]) }
                .filter { it.isNotBlank() }
                .toList()
            if (cells.isEmpty()) return@forEach
            val line = if (cells.size == 2) {
                "${cells[0]}: ${cells[1]}"
            } else {
                cells.joinToString(" | ")
            }
            if (line.isNotBlank()) lines.add(line)
        }
        return lines
    }

    private fun clean(value: String): String {
        return value
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace(Regex("\\s+"), " ")
            .trim()
    }
}
