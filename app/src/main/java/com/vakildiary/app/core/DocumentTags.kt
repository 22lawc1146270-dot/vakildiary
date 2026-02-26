package com.vakildiary.app.core

object DocumentTags {
    private const val KEY_JUDGMENT = "judgment"
    private const val KEY_SCID = "scid"
    private const val KEY_YEAR = "year"
    private const val KEY_CASE = "case"
    private const val KEY_REPORTABLE = "reportable"
    private const val SEPARATOR = "|"

    fun buildJudgmentTags(
        judgmentId: String,
        year: String?,
        caseNumber: String?,
        reportable: Boolean
    ): String {
        val parts = mutableListOf(KEY_JUDGMENT)
        if (judgmentId.isNotBlank()) parts.add("$KEY_SCID=$judgmentId")
        if (!year.isNullOrBlank()) parts.add("$KEY_YEAR=${year.trim()}")
        if (!caseNumber.isNullOrBlank()) parts.add("$KEY_CASE=${caseNumber.trim()}")
        parts.add("$KEY_REPORTABLE=$reportable")
        return parts.joinToString(SEPARATOR)
    }

    fun isJudgment(tags: String): Boolean {
        return tags.split(SEPARATOR).any { it.trim().equals(KEY_JUDGMENT, ignoreCase = true) }
    }

    fun isReportable(tags: String): Boolean {
        return parseTags(tags)[KEY_REPORTABLE]?.equals("true", ignoreCase = true) == true
    }

    fun judgmentId(tags: String): String? = parseTags(tags)[KEY_SCID]

    fun judgmentYear(tags: String): String? = parseTags(tags)[KEY_YEAR]

    fun judgmentCaseNumber(tags: String): String? = parseTags(tags)[KEY_CASE]

    private fun parseTags(tags: String): Map<String, String> {
        return tags.split(SEPARATOR)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .mapNotNull { part ->
                val idx = part.indexOf('=')
                if (idx <= 0) return@mapNotNull null
                val key = part.substring(0, idx).trim()
                val value = part.substring(idx + 1).trim()
                if (key.isBlank() || value.isBlank()) null else key to value
            }
            .toMap()
    }
}
