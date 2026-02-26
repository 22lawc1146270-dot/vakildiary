package com.vakildiary.app.presentation.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vakildiary.app.core.DocumentTags
import com.vakildiary.app.core.Result
import com.vakildiary.app.domain.model.Document
import com.vakildiary.app.domain.usecase.documents.AttachDocumentUseCase
import com.vakildiary.app.presentation.model.ReportableCaseTypeOption
import com.vakildiary.app.presentation.model.ReportableFormData
import com.vakildiary.app.presentation.model.ReportableFormFields
import com.vakildiary.app.presentation.model.ReportableFormInput
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject

@HiltViewModel
class ReportableJudgmentViewModel @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val attachDocumentUseCase: AttachDocumentUseCase
) : ViewModel() {
    private val _formState = MutableStateFlow<ReportableFormUiState>(ReportableFormUiState.Loading)
    val formState: StateFlow<ReportableFormUiState> = _formState.asStateFlow()

    private val _downloadState = MutableStateFlow<ReportableDownloadUiState>(ReportableDownloadUiState.Idle)
    val downloadState: StateFlow<ReportableDownloadUiState> = _downloadState.asStateFlow()

    fun loadForm(caseNumber: String?, year: String?) {
        viewModelScope.launch {
            _formState.value = ReportableFormUiState.Loading
            _formState.value = when (val result = fetchFormData(caseNumber, year)) {
                is Result.Success -> ReportableFormUiState.Success(result.data)
                is Result.Error -> ReportableFormUiState.Error(result.message)
            }
        }
    }

    fun submitForm(
        input: ReportableFormInput,
        judgmentId: String,
        year: String?,
        caseNumber: String?,
        requiredMessage: String
    ) {
        val current = (_formState.value as? ReportableFormUiState.Success)?.data
            ?: return
        if (input.caseTypeValue.isBlank() ||
            input.caseNumber.isBlank() ||
            input.captcha.isBlank() ||
            (current.fields.caseYear != null && input.caseYear.isNullOrBlank()) ||
            (current.fields.diaryYear != null && input.diaryYear.isNullOrBlank())
        ) {
            _downloadState.value = ReportableDownloadUiState.Error(requiredMessage)
            return
        }
        if (_downloadState.value is ReportableDownloadUiState.Loading) return
        viewModelScope.launch {
            _downloadState.value = ReportableDownloadUiState.Loading
            _downloadState.value = withContext(Dispatchers.IO) {
                when (val result = submitFormRequest(current, input)) {
                    is Result.Success -> downloadReportablePdf(
                        url = result.data,
                        judgmentId = judgmentId,
                        year = year,
                        caseNumber = caseNumber
                    )
                    is Result.Error -> ReportableDownloadUiState.Error(result.message)
                }
            }
        }
    }

    fun resetState() {
        _downloadState.value = ReportableDownloadUiState.Idle
    }

    private suspend fun fetchFormData(
        caseNumber: String?,
        year: String?
    ): Result<ReportableFormData> = withContext(Dispatchers.IO) {
        when (val htmlResult = fetchHtml(SCI_CASE_URL)) {
            is Result.Error -> htmlResult
            is Result.Success -> {
                val html = htmlResult.data
                val actionUrl = parseFormAction(html) ?: SCI_CASE_URL
                val labels = parseLabels(html)
                val inputs = parseInputs(html)
                val selects = parseSelects(html)
                val caseTypeName = findSelectName(selects, labels, listOf("case", "type"))
                    ?: return@withContext Result.Error("Case type field missing")
                val caseNumberName = findInputName(
                    inputs,
                    labels,
                    listOf("case", "number"),
                    listOf("case_no", "case_number")
                ) ?: return@withContext Result.Error("Case number field missing")
                val caseYearName = findInputName(
                    inputs,
                    labels,
                    listOf("case", "year"),
                    listOf("case_year", "case_yr", "caseyear")
                )
                val diaryYearName = findInputName(
                    inputs,
                    labels,
                    listOf("diary", "year"),
                    listOf("diary_year", "diary_yr", "diaryyear")
                )
                val captchaName = findInputName(inputs, labels, listOf("captcha"), listOf("captcha", "answer"))
                    ?: return@withContext Result.Error("Captcha field missing")
                val caseTypes = parseCaseTypeOptions(html, caseTypeName)
                val hiddenFields = parseHiddenFields(inputs)
                val captchaUrl = parseCaptchaUrl(html)
                    ?: return@withContext Result.Error("Captcha image unavailable")
                when (val captchaResult = fetchBytes(resolveSciUrl(captchaUrl))) {
                    is Result.Error -> captchaResult
                    is Result.Success -> {
                        val suggested = suggestCaseTypeValue(caseNumber, caseTypes)
                        Result.Success(
                            ReportableFormData(
                                actionUrl = actionUrl,
                                caseTypes = caseTypes,
                                fields = ReportableFormFields(
                                    caseType = caseTypeName,
                                    caseNumber = caseNumberName,
                                    caseYear = caseYearName,
                                    diaryYear = diaryYearName,
                                    captcha = captchaName
                                ),
                                hiddenFields = hiddenFields,
                                captchaImage = captchaResult.data,
                                suggestedCaseTypeValue = suggested
                            )
                        )
                    }
                }
            }
        }
    }

    private suspend fun submitFormRequest(
        form: ReportableFormData,
        input: ReportableFormInput
    ): Result<String> {
        val formBodyBuilder = FormBody.Builder()
        form.hiddenFields.forEach { (key, value) ->
            formBodyBuilder.add(key, value)
        }
        formBodyBuilder.add(form.fields.caseType, input.caseTypeValue)
        formBodyBuilder.add(form.fields.caseNumber, input.caseNumber)
        form.fields.caseYear?.let { field -> input.caseYear?.let { formBodyBuilder.add(field, it) } }
        form.fields.diaryYear?.let { field -> input.diaryYear?.let { formBodyBuilder.add(field, it) } }
        formBodyBuilder.add(form.fields.captcha, input.captcha)
        val request = Request.Builder()
            .url(form.actionUrl)
            .post(formBodyBuilder.build())
            .header("Referer", SCI_CASE_URL)
            .build()
        return executeHtml(request)
            .map { html ->
                parsePdfLink(html)?.let { resolveSciUrl(it) }
                    ?: return Result.Error("Reportable PDF link not found")
            }
    }

    private suspend fun downloadReportablePdf(
        url: String,
        judgmentId: String,
        year: String?,
        caseNumber: String?
    ): ReportableDownloadUiState {
        val request = Request.Builder().url(url).build()
        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return ReportableDownloadUiState.Error("Reportable download failed (HTTP ${response.code})")
                }
                val body = response.body ?: return ReportableDownloadUiState.Error("Reportable download failed")
                val fileName = resolveFileName(url, response) ?: "reportable_${judgmentId}.pdf"
                val mimeType = body.contentType()?.toString() ?: "application/pdf"
                val tags = DocumentTags.buildJudgmentTags(
                    judgmentId = judgmentId,
                    year = year,
                    caseNumber = caseNumber,
                    reportable = true
                )
                return when (val result = attachDocumentUseCase(
                    caseId = null,
                    fileName = fileName,
                    mimeType = mimeType,
                    inputStreamProvider = { body.byteStream() },
                    isScanned = false,
                    tags = tags
                )) {
                    is Result.Success -> ReportableDownloadUiState.Success(result.data)
                    is Result.Error -> ReportableDownloadUiState.Error(result.message)
                }
            }
        } catch (t: Throwable) {
            ReportableDownloadUiState.Error("Reportable download failed")
        }
    }

    private suspend fun fetchHtml(url: String): Result<String> {
        val request = Request.Builder().url(url).get().build()
        return executeHtml(request)
    }

    private suspend fun executeHtml(request: Request): Result<String> {
        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.Error("Request failed (HTTP ${response.code})")
                }
                val html = response.body?.string().orEmpty()
                if (html.isBlank()) {
                    Result.Error("Empty response")
                } else {
                    Result.Success(html)
                }
            }
        } catch (t: Throwable) {
            val message = if (t is IOException) "Network error" else "Request failed"
            Result.Error(message, t)
        }
    }

    private suspend fun fetchBytes(url: String): Result<ByteArray> {
        val request = Request.Builder().url(url).get().build()
        return try {
            okHttpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return Result.Error("Captcha download failed (HTTP ${response.code})")
                }
                val body = response.body ?: return Result.Error("Captcha download failed")
                Result.Success(body.bytes())
            }
        } catch (t: Throwable) {
            Result.Error("Captcha download failed", t)
        }
    }

    private fun parseFormAction(html: String): String? {
        val match = Regex(
            "<form[^>]*action=['\"]([^'\"]+)['\"][^>]*>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(html)
            ?: return null
        return resolveSciUrl(match.groupValues[1])
    }

    private fun parseLabels(html: String): Map<String, String> {
        val labelRegex = Regex(
            "<label[^>]*for=['\"]([^'\"]+)['\"][^>]*>(.*?)</label>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        return labelRegex.findAll(html).mapNotNull { match ->
            val id = match.groupValues[1].trim()
            val text = clean(match.groupValues[2])
            if (id.isBlank() || text.isBlank()) null else id to text
        }.toMap()
    }

    private data class HtmlInput(
        val name: String?,
        val id: String?,
        val type: String?,
        val value: String?
    )

    private data class HtmlSelect(
        val name: String?,
        val id: String?
    )

    private fun parseInputs(html: String): List<HtmlInput> {
        val regex = Regex("<input[^>]*>", RegexOption.IGNORE_CASE)
        return regex.findAll(html).map { match ->
            val attrs = parseAttributes(match.value)
            HtmlInput(
                name = attrs["name"],
                id = attrs["id"],
                type = attrs["type"],
                value = attrs["value"]
            )
        }.toList()
    }

    private fun parseSelects(html: String): List<HtmlSelect> {
        val regex = Regex("<select[^>]*>", RegexOption.IGNORE_CASE)
        return regex.findAll(html).map { match ->
            val attrs = parseAttributes(match.value)
            HtmlSelect(
                name = attrs["name"],
                id = attrs["id"]
            )
        }.toList()
    }

    private fun parseHiddenFields(inputs: List<HtmlInput>): Map<String, String> {
        return inputs.filter { it.type.equals("hidden", ignoreCase = true) }
            .mapNotNull { input ->
                val name = input.name?.trim().orEmpty()
                if (name.isBlank()) return@mapNotNull null
                name to input.value.orEmpty()
            }.toMap()
    }

    private fun parseCaseTypeOptions(html: String, selectName: String): List<ReportableCaseTypeOption> {
        val selectRegex = Regex(
            "<select[^>]*name=['\"]${Regex.escape(selectName)}['\"][^>]*>(.*?)</select>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val selectMatch = selectRegex.find(html) ?: return emptyList()
        val optionRegex = Regex(
            "<option[^>]*value=['\"]?([^'\">\\s]+)['\"]?[^>]*>(.*?)</option>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        return optionRegex.findAll(selectMatch.groupValues[1]).mapNotNull { match ->
            val value = match.groupValues[1].trim()
            val label = clean(match.groupValues[2])
            if (value.isBlank() || label.isBlank() || label.startsWith("select", ignoreCase = true)) {
                null
            } else {
                ReportableCaseTypeOption(value = value, label = label)
            }
        }.toList()
    }

    private fun parseCaptchaUrl(html: String): String? {
        val imgRegex = Regex(
            "<img[^>]*>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        )
        val candidates = imgRegex.findAll(html).map { it.value }.toList()
        val captchaTag = candidates.firstOrNull { tag ->
            tag.contains("captcha", ignoreCase = true)
        } ?: return null
        return parseAttributes(captchaTag)["src"]
    }

    private fun findInputName(
        inputs: List<HtmlInput>,
        labels: Map<String, String>,
        labelKeywords: List<String>,
        nameKeywords: List<String>
    ): String? {
        inputs.firstOrNull { input ->
            val label = input.id?.let { labels[it] }
            label != null && labelKeywords.all { label.contains(it, ignoreCase = true) }
        }?.name?.let { return it }
        return inputs.firstOrNull { input ->
            val name = input.name.orEmpty()
            nameKeywords.any { name.contains(it, ignoreCase = true) }
        }?.name
    }

    private fun findSelectName(
        selects: List<HtmlSelect>,
        labels: Map<String, String>,
        labelKeywords: List<String>
    ): String? {
        selects.firstOrNull { select ->
            val label = select.id?.let { labels[it] }
            label != null && labelKeywords.all { label.contains(it, ignoreCase = true) }
        }?.name?.let { return it }
        return selects.firstOrNull { select ->
            val name = select.name.orEmpty()
            labelKeywords.all { name.contains(it, ignoreCase = true) }
        }?.name
    }

    private fun suggestCaseTypeValue(
        caseNumber: String?,
        options: List<ReportableCaseTypeOption>
    ): String? {
        val label = extractCaseTypeLabel(caseNumber) ?: return null
        return options.firstOrNull { it.label.contains(label, ignoreCase = true) }?.value
    }

    private fun extractCaseTypeLabel(caseNumber: String?): String? {
        if (caseNumber.isNullOrBlank()) return null
        val normalized = caseNumber.replace(Regex("\\s+"), " ").trim()
        val match = Regex(
            "([A-Za-z][A-Za-z\\s&.-]+)\\s*(No\\.|No|Nos\\.|Nos|Number)",
            RegexOption.IGNORE_CASE
        ).find(normalized)
        return match?.groupValues?.getOrNull(1)?.trim()
    }

    private fun parsePdfLink(html: String): String? {
        val rowMatch = Regex(
            "<tr[^>]*id=['\"]f8efc002962d889bc2b0419a271c6d['\"][^>]*>(.*?)</tr>",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
        ).find(html)
        val rowHtml = rowMatch?.groupValues?.getOrNull(1)
        if (!rowHtml.isNullOrBlank()) {
            val linkRegex = Regex(
                "<a[^>]*href=['\"]([^'\"]+)['\"][^>]*>",
                setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL)
            )
            val links = linkRegex.findAll(rowHtml).map { it.groupValues[1] }.toList()
            if (links.size >= 3) return links[2]
        }
        val fallback = Regex("href=['\"]([^'\"]+\\.pdf[^'\"]*)['\"]", RegexOption.IGNORE_CASE)
            .find(html)
            ?.groupValues
            ?.getOrNull(1)
        return fallback
    }

    private fun resolveSciUrl(raw: String): String {
        val cleaned = raw.replace("&amp;", "&").trim()
        return when {
            cleaned.startsWith("http", ignoreCase = true) -> cleaned
            cleaned.startsWith("/") -> "https://www.sci.gov.in${cleaned}"
            else -> "https://www.sci.gov.in/$cleaned"
        }
    }

    private fun parseAttributes(tag: String): Map<String, String> {
        val attrRegex = Regex("(\\w+)\\s*=\\s*['\"]([^'\"]*)['\"]")
        return attrRegex.findAll(tag).associate { match ->
            match.groupValues[1].lowercase() to match.groupValues[2]
        }
    }

    private fun resolveFileName(url: String, response: Response): String? {
        val disposition = response.header("Content-Disposition")
        val nameFromHeader = disposition?.let {
            Regex("filename=\\\"?([^\\\";]+)").find(it)?.groupValues?.getOrNull(1)
        }?.trim()
        if (!nameFromHeader.isNullOrBlank()) return nameFromHeader
        val trimmed = url.substringBefore('?')
        return trimmed.substringAfterLast('/').takeIf { it.isNotBlank() }
    }

    private fun clean(value: String): String {
        return value
            .replace(Regex("<[^>]+>"), " ")
            .replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    companion object {
        private const val SCI_CASE_URL = "https://www.sci.gov.in/judgements-case-no/"
    }
}

sealed interface ReportableFormUiState {
    object Loading : ReportableFormUiState
    data class Success(val data: ReportableFormData) : ReportableFormUiState
    data class Error(val message: String) : ReportableFormUiState
}

sealed interface ReportableDownloadUiState {
    object Idle : ReportableDownloadUiState
    object Loading : ReportableDownloadUiState
    data class Success(val document: Document) : ReportableDownloadUiState
    data class Error(val message: String) : ReportableDownloadUiState
}

private inline fun <T, R> Result<T>.map(transform: (T) -> R): Result<R> {
    return when (this) {
        is Result.Success -> Result.Success(transform(data))
        is Result.Error -> Result.Error(message, throwable)
    }
}
