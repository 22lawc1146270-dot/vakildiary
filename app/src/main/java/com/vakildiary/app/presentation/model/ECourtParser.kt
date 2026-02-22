package com.vakildiary.app.presentation.model

import com.vakildiary.app.presentation.model.ECourtCaseItem

object ECourtParser {
    fun parse(raw: String, form: ECourtSearchForm): List<ECourtCaseItem> {
        val rowRegex = Regex("<tr[^>]*>(.*?)</tr>", RegexOption.IGNORE_CASE or RegexOption.DOT_MATCHES_ALL)
        val cellRegex = Regex("<td[^>]*>(.*?)</td>", RegexOption.IGNORE_CASE or RegexOption.DOT_MATCHES_ALL)
        val tagRegex = Regex("<[^>]+>")
        val dateRegex = Regex("\\b\\d{2}/\\d{2}/\\d{4}\\b")

        fun clean(value: String): String {
            return tagRegex.replace(value, "")
                .replace("&nbsp;", " ")
                .replace("&amp;", "&")
                .trim()
        }

        val items = mutableListOf<ECourtCaseItem>()
        rowRegex.findAll(raw).forEach { match ->
            val row = match.groupValues[1]
            val cells = cellRegex.findAll(row).map { clean(it.groupValues[1]) }
                .filter { it.isNotBlank() }
                .toList()
            if (cells.isEmpty()) return@forEach
            val startIndex = if (cells.first().matches(Regex("\\d+"))) 1 else 0
            val title = cells.getOrNull(startIndex).orEmpty()
            val parties = cells.getOrNull(startIndex + 1).orEmpty()
            val stage = cells.lastOrNull().orEmpty()
            val date = cells.firstOrNull { dateRegex.matches(it) }
                ?: dateRegex.find(row)?.value
                ?: ""
            val caseNumber = cells.firstOrNull { it.contains("/") || it.contains("-") }
                ?: form.caseNumber
            val clientName = extractClientName(parties)
            if (title.isBlank() && parties.isBlank()) return@forEach
            items.add(
                ECourtCaseItem(
                    caseNumber = caseNumber,
                    caseTitle = if (title.isBlank()) caseNumber else title,
                    parties = parties,
                    nextHearingDate = date,
                    stage = stage,
                    courtName = form.courtName,
                    courtType = form.courtType,
                    clientName = clientName
                )
            )
        }
        if (items.isEmpty()) {
            return listOf(
                ECourtCaseItem(
                    caseNumber = form.caseNumber,
                    caseTitle = "Case ${form.caseNumber}",
                    parties = "",
                    nextHearingDate = "",
                    stage = "",
                    courtName = form.courtName,
                    courtType = form.courtType,
                    clientName = ""
                )
            )
        }
        return items
    }

    private fun extractClientName(parties: String): String {
        val normalized = parties.replace("Vs.", "vs", ignoreCase = true)
            .replace("V.", "vs", ignoreCase = true)
            .replace("v.", "vs", ignoreCase = true)
        return normalized.split("vs", ignoreCase = true)
            .firstOrNull()
            ?.trim()
            .orEmpty()
    }
}
