package com.vakildiary.app.data.ecourt

import com.vakildiary.app.domain.model.ECourtComplexOption
import com.vakildiary.app.domain.model.ECourtOption

object ECourtHtmlParser {
    private const val BASE_URL = "https://services.ecourts.gov.in"
    private val optionRegex = Regex(
        "<option[^>]*value\\s*=\\s*['\"]?([^'\">\\s]+)['\"]?[^>]*>(.*?)</option>",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
    )
    private val tagRegex = Regex("<[^>]+>")

    fun parseAppToken(html: String): String? {
        return Regex("app_token=([a-f0-9]{32,})", RegexOption.IGNORE_CASE)
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
    }

    fun parseStateOptions(html: String): List<ECourtOption> {
        val selectMatch = Regex(
            "<select[^>]*id=['\"]sess_state_code['\"][^>]*>(.*?)</select>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(html) ?: return emptyList()
        return parseOptions(selectMatch.groupValues[1])
    }

    fun parseOptions(html: String): List<ECourtOption> {
        return optionRegex.findAll(html).mapNotNull { match ->
            val code = match.groupValues[1].trim()
            val label = clean(match.groupValues[2])
            if (code.isBlank() || label.isBlank() || label.startsWith("Select", ignoreCase = true)) {
                null
            } else {
                ECourtOption(code = code, label = label)
            }
        }.toList()
    }

    fun parseComplexOptions(html: String): List<ECourtComplexOption> {
        return optionRegex.findAll(html).mapNotNull { match ->
            val rawCode = match.groupValues[1].trim()
            val label = clean(match.groupValues[2])
            if (rawCode.isBlank() || label.isBlank() || label.startsWith("Select", ignoreCase = true)) {
                return@mapNotNull null
            }
            val parts = rawCode.split("@")
            val complexCode = parts.getOrNull(0).orEmpty()
            val courtCodes = parts.getOrNull(1).orEmpty()
            val requiresEstablishment = parts.getOrNull(2)?.equals("Y", ignoreCase = true) == true
            if (complexCode.isBlank()) return@mapNotNull null
            ECourtComplexOption(
                complexCode = complexCode,
                courtCodes = courtCodes,
                requiresEstablishment = requiresEstablishment,
                label = label
            )
        }.toList()
    }

    fun parseCaptchaImageUrl(html: String): String? {
        val normalized = html
            .replace("\\\"", "\"")
            .replace("&quot;", "\"")
            .replace("&#34;", "\"")
            .replace("\\/", "/")
        val match = Regex(
            "src\\s*=\\s*['\"]?([^'\"\\s>]*securimage_show\\.php[^'\"\\s>]*)",
            RegexOption.IGNORE_CASE
        ).find(normalized)
            ?.groupValues
            ?.getOrNull(1)
            ?: return null
        val cleaned = match.replace("\\/", "/")
        return if (cleaned.startsWith("http")) cleaned else "$BASE_URL/${cleaned.trimStart('/')}"
    }

    private fun clean(value: String): String {
        return tagRegex.replace(value, "")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .trim()
    }
}
