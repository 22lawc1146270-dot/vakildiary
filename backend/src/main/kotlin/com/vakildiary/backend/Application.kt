package com.vakildiary.backend

import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.Page
import com.microsoft.playwright.Playwright
import com.microsoft.playwright.options.LoadState
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.request.receive
import io.ktor.server.plugins.origin
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.routing.get
import io.ktor.server.routing.post
import io.ktor.server.routing.routing
import io.ktor.serialization.gson.gson
import java.net.HttpURLConnection
import java.net.URL
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

private const val SCI_CASE_URL = "https://www.sci.gov.in/judgements-case-no/"
private const val SCI_ORIGIN = "https://www.sci.gov.in"
private const val SCI_REFERER = "https://www.sci.gov.in/judgements-case-no/"
private const val MOBILE_USER_AGENT =
    "Mozilla/5.0 (Linux; Android 14; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Mobile Safari/537.36"
private const val SESSION_TTL_MILLIS = 15 * 60 * 1000L

fun main() {
    embeddedServer(Netty, port = 8080, module = Application::module)
        .start(wait = true)
}

fun Application.module() {
    install(ContentNegotiation) { gson() }
    install(CallLogging)

    routing {
        get("/api/reportable/form") {
            ReportableSessionStore.cleanupExpired()
            val caseNumber = call.request.queryParameters["caseNumber"]
            val year = call.request.queryParameters["year"]
            try {
                val session = ReportableSessionStore.create(caseNumber, year)
                call.respond(session.toFormResponse())
            } catch (t: Throwable) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Failed to load form"))
            }
        }

        post("/api/reportable/submit") {
            ReportableSessionStore.cleanupExpired()
            try {
                val input = call.receive<ReportableSubmitRequest>()
                val session = ReportableSessionStore.get(input.sessionId)
                    ?: return@post call.respond(HttpStatusCode.BadRequest, ErrorResponse("Session expired"))
                val origin = "${call.request.origin.scheme}://${call.request.origin.host}:${call.request.origin.port}"
                val response = ReportableSessionStore.submit(session, input, origin)
                call.respond(response)
            } catch (t: Throwable) {
                call.respond(HttpStatusCode.BadRequest, ErrorResponse("Reportable submission failed"))
            }
        }

        get("/api/reportable/download/{sessionId}") {
            ReportableSessionStore.cleanupExpired()
            val sessionId = call.parameters["sessionId"].orEmpty()
            val session = ReportableSessionStore.get(sessionId)
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Session expired"))
            val downloadUrl = session.downloadUrl
                ?: return@get call.respond(HttpStatusCode.BadRequest, ErrorResponse("Download not ready"))
            val fileName = session.fileName ?: "reportable_$sessionId.pdf"
            val cookieHeader = session.cookieHeader
            call.response.headers.append(
                "Content-Disposition",
                "attachment; filename=\"$fileName\""
            )
            try {
                call.respondOutputStream(
                    contentType = ContentType.Application.Pdf,
                    status = HttpStatusCode.OK
                ) {
                    streamPdf(downloadUrl, cookieHeader, outputStream = this)
                }
            } finally {
                ReportableSessionStore.remove(sessionId)
            }
        }
    }
}

private object ReportableSessionStore {
    private val sessions = ConcurrentHashMap<String, ReportableSession>()
    private val playwright: Playwright by lazy { Playwright.create() }
    private val browser: Browser by lazy { playwright.chromium().launch() }

    fun cleanupExpired() {
        val now = System.currentTimeMillis()
        sessions.values.filter { now - it.createdAt > SESSION_TTL_MILLIS }.forEach { session ->
            remove(session.sessionId)
        }
    }

    fun create(caseNumber: String?, year: String?): ReportableSession {
        val context = browser.newContext(
            Browser.NewContextOptions()
                .setUserAgent(MOBILE_USER_AGENT)
                .setViewportSize(412, 915)
                .setLocale("en-IN")
                .setExtraHTTPHeaders(
                    mapOf(
                        "Accept" to "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8",
                        "Accept-Language" to "en-IN,en;q=0.9",
                        "Origin" to SCI_ORIGIN,
                        "Referer" to SCI_REFERER,
                        "Upgrade-Insecure-Requests" to "1"
                    )
                )
        )
        val page = context.newPage()
        page.navigate(SCI_CASE_URL)
        page.waitForLoadState(LoadState.DOMCONTENTLOADED)
        val html = page.content()
        val labels = parseLabels(html)
        val inputs = parseInputs(html)
        val selects = parseSelects(html)
        val caseTypeName = findSelectName(selects, labels, listOf("case", "type"))
            ?: throw IllegalStateException("Case type field missing")
        val caseNumberName = findInputName(
            inputs,
            labels,
            listOf("case", "number"),
            listOf("case_no", "case_number")
        ) ?: throw IllegalStateException("Case number field missing")
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
            ?: throw IllegalStateException("Captcha field missing")
        val caseTypes = parseCaseTypeOptions(html, caseTypeName)
        val captchaUrl = parseCaptchaUrl(html)
            ?: throw IllegalStateException("Captcha image unavailable")
        val captchaBytes = fetchBytes(context, resolveSciUrl(captchaUrl))
        val suggested = suggestCaseTypeValue(caseNumber, caseTypes)
        val sessionId = UUID.randomUUID().toString()
        val session = ReportableSession(
            sessionId = sessionId,
            createdAt = System.currentTimeMillis(),
            context = context,
            page = page,
            fields = ReportableFormFields(
                caseType = caseTypeName,
                caseNumber = caseNumberName,
                caseYear = caseYearName,
                diaryYear = diaryYearName,
                captcha = captchaName
            ),
            caseTypes = caseTypes,
            requiresCaseYear = caseYearName != null,
            requiresDiaryYear = diaryYearName != null,
            captchaImage = captchaBytes,
            suggestedCaseTypeValue = suggested
        )
        sessions[sessionId] = session
        return session
    }

    fun get(sessionId: String): ReportableSession? = sessions[sessionId]

    fun submit(
        session: ReportableSession,
        input: ReportableSubmitRequest,
        origin: String
    ): ReportableSubmitResponse {
        val page = session.page ?: throw IllegalStateException("Session missing page")
        page.selectOption("select[name='${session.fields.caseType}']", input.caseTypeValue)
        page.fill("input[name='${session.fields.caseNumber}']", input.caseNumber)
        session.fields.caseYear?.let { name ->
            input.caseYear?.let { page.fill("input[name='$name']", it) }
        }
        session.fields.diaryYear?.let { name ->
            input.diaryYear?.let { page.fill("input[name='$name']", it) }
        }
        page.fill("input[name='${session.fields.captcha}']", input.captcha)
        page.evaluate(
            "caseTypeName => {" +
                "const select = document.querySelector(\"select[name='\" + caseTypeName + \"']\");" +
                "const form = select ? select.closest('form') : document.querySelector('form');" +
                "if (form) form.submit();" +
                "}",
            session.fields.caseType
        )
        page.waitForLoadState(LoadState.DOMCONTENTLOADED)
        val html = page.content()
        val pdfLink = parsePdfLink(html) ?: throw IllegalStateException("Reportable PDF link not found")
        val downloadUrl = resolveSciUrl(pdfLink)
        val fileName = resolveFileName(downloadUrl)
        val cookieHeader = session.context?.cookies()?.joinToString("; ") { "${it.name}=${it.value}" }
        session.downloadUrl = downloadUrl
        session.fileName = fileName
        session.cookieHeader = cookieHeader
        session.page?.close()
        session.context?.close()
        session.page = null
        session.context = null
        val downloadEndpoint = "$origin/api/reportable/download/${session.sessionId}"
        return ReportableSubmitResponse(
            downloadUrl = downloadEndpoint,
            fileName = fileName
        )
    }

    fun remove(sessionId: String) {
        sessions.remove(sessionId)?.let { session ->
            session.page?.close()
            session.context?.close()
        }
    }

    private fun fetchBytes(context: BrowserContext, url: String): ByteArray {
        val response = context.request().get(url)
        if (!response.ok()) {
            throw IllegalStateException("Captcha download failed")
        }
        return response.body()
    }

    private fun resolveFileName(url: String): String {
        val trimmed = url.substringBefore('?')
        return trimmed.substringAfterLast('/').takeIf { it.isNotBlank() } ?: "reportable.pdf"
    }

    private fun resolveSciUrl(raw: String): String {
        val cleaned = raw.replace("&amp;", "&").trim()
        return when {
            cleaned.startsWith("http", ignoreCase = true) -> cleaned
            cleaned.startsWith("/") -> "https://www.sci.gov.in$cleaned"
            else -> "https://www.sci.gov.in/$cleaned"
        }
    }
}

private fun streamPdf(url: String, cookieHeader: String?, outputStream: java.io.OutputStream) {
    val connection = (URL(url).openConnection() as HttpURLConnection).apply {
        setRequestProperty("User-Agent", MOBILE_USER_AGENT)
        setRequestProperty("Accept", "application/pdf,application/octet-stream;q=0.9,*/*;q=0.8")
        setRequestProperty("Accept-Language", "en-IN,en;q=0.9")
        setRequestProperty("Referer", SCI_REFERER)
        if (!cookieHeader.isNullOrBlank()) {
            setRequestProperty("Cookie", cookieHeader)
        }
    }
    if (connection.responseCode != HttpURLConnection.HTTP_OK) {
        throw IllegalStateException("Download failed (HTTP ${connection.responseCode})")
    }
    connection.inputStream.use { input ->
        input.copyTo(outputStream)
    }
}

private data class ReportableFormFields(
    val caseType: String,
    val caseNumber: String,
    val caseYear: String?,
    val diaryYear: String?,
    val captcha: String
)

private data class ReportableSession(
    val sessionId: String,
    val createdAt: Long,
    var context: BrowserContext?,
    var page: Page?,
    val fields: ReportableFormFields,
    val caseTypes: List<ReportableCaseTypeOption>,
    val requiresCaseYear: Boolean,
    val requiresDiaryYear: Boolean,
    val captchaImage: ByteArray,
    val suggestedCaseTypeValue: String?
) {
    var downloadUrl: String? = null
    var fileName: String? = null
    var cookieHeader: String? = null
    fun toFormResponse(): ReportableFormResponse {
        return ReportableFormResponse(
            sessionId = sessionId,
            caseTypes = caseTypes,
            requiresCaseYear = requiresCaseYear,
            requiresDiaryYear = requiresDiaryYear,
            captchaImageBase64 = java.util.Base64.getEncoder().encodeToString(captchaImage),
            suggestedCaseTypeValue = suggestedCaseTypeValue
        )
    }
}

private data class ReportableCaseTypeOption(
    val value: String,
    val label: String
)

private data class ReportableFormResponse(
    val sessionId: String,
    val caseTypes: List<ReportableCaseTypeOption>,
    val requiresCaseYear: Boolean,
    val requiresDiaryYear: Boolean,
    val captchaImageBase64: String,
    val suggestedCaseTypeValue: String?
)

private data class ReportableSubmitRequest(
    val sessionId: String,
    val caseTypeValue: String,
    val caseNumber: String,
    val caseYear: String?,
    val diaryYear: String?,
    val captcha: String
)

private data class ReportableSubmitResponse(
    val downloadUrl: String,
    val fileName: String?
)

private data class ErrorResponse(val message: String)

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

private fun parseAttributes(tag: String): Map<String, String> {
    val attrRegex = Regex("(\\w+)\\s*=\\s*['\"]([^'\"]*)['\"]")
    return attrRegex.findAll(tag).associate { match ->
        match.groupValues[1].lowercase() to match.groupValues[2]
    }
}

private fun clean(value: String): String {
    return value
        .replace(Regex("<[^>]+>"), " ")
        .replace("&nbsp;", " ")
        .replace("&amp;", "&")
        .replace(Regex("\\s+"), " ")
        .trim()
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
