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
        val caseDetails = extractSection(raw, "Case Details")
        val caseStatus = extractSection(raw, "Case Status")
        val petitionerAdvocate = extractSection(raw, "Petitioner and Advocate")
        val respondentAdvocate = extractSection(raw, "Respondent and Advocate")
        val acts = extractSection(raw, "Acts")
        val caseHistory = extractSection(raw, "Case History")
        val transferDetails = extractSection(raw, "Case Transfer Details within Establishment")
        return ECourtCaseDetails(
            caseTitle = item.caseTitle,
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
