package com.vakildiary.app.presentation.screens.judgments

import android.annotation.SuppressLint
import android.webkit.JavascriptInterface
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.R
import com.vakildiary.app.presentation.components.AnimatedSuccessDialog
import com.vakildiary.app.presentation.viewmodels.ReportableDownloadUiState
import com.vakildiary.app.presentation.viewmodels.ReportableJudgmentViewModel
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@SuppressLint("SetJavaScriptEnabled")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportableJudgmentScreen(
    judgmentId: String,
    caseNumber: String?,
    year: String?,
    petitionerName: String?,
    judgmentDate: String?,
    onBack: () -> Unit,
    onOpenDocuments: () -> Unit,
    viewModel: ReportableJudgmentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val showInfoDialog by viewModel.showFreeTextInfoDialog.collectAsStateWithLifecycle()

    var dontShowAgain by remember { mutableStateOf(false) }
    var autoFillWarningShown by remember { mutableStateOf(false) }
    var noPdfWarningShown by remember { mutableStateOf(false) }
    var downloadTriggered by remember { mutableStateOf(false) }
    var webViewRef by remember { mutableStateOf<WebView?>(null) }

    val freeTextQuery = remember(petitionerName, caseNumber) {
        resolveFreeTextQuery(petitionerName, caseNumber)
    }
    val dateRange = remember(judgmentDate, year) {
        resolveDateRange(judgmentDate, year)
    }

    LaunchedEffect(Unit) {
        viewModel.prepareFreeTextInfoDialog()
    }

    DisposableEffect(Unit) {
        onDispose {
            webViewRef?.removeJavascriptInterface(JS_BRIDGE_NAME)
            webViewRef?.stopLoading()
            webViewRef?.destroy()
            webViewRef = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = stringResource(id = R.string.reportable_judgment_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
        ) {
            Text(
                text = stringResource(id = R.string.reportable_manual_steps_hint),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
            )

            if (!showInfoDialog) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        WebView(ctx).apply {
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.loadsImagesAutomatically = true
                            settings.javaScriptCanOpenWindowsAutomatically = true
                            addJavascriptInterface(
                                SciResultBridge(
                                    onResultsReady = {
                                        post {
                                            attemptAutoOpenFirstResult(this) { outcome ->
                                                if (outcome.contains("no_pdf", ignoreCase = true) &&
                                                    !noPdfWarningShown
                                                ) {
                                                    noPdfWarningShown = true
                                                    Toast.makeText(
                                                        context,
                                                        context.getString(R.string.reportable_no_pdf_found),
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                ),
                                JS_BRIDGE_NAME
                            )
                            webViewClient = object : WebViewClient() {
                                override fun shouldOverrideUrlLoading(
                                    view: WebView?,
                                    request: WebResourceRequest?
                                ): Boolean {
                                    val targetUrl = request?.url?.toString().orEmpty()
                                    if (!isPdfUrl(targetUrl)) return false
                                    if (!downloadTriggered) {
                                        downloadTriggered = true
                                        viewModel.downloadFromPdfUrl(
                                            downloadUrl = targetUrl,
                                            judgmentId = judgmentId,
                                            year = year,
                                            caseNumber = caseNumber,
                                            petitionerName = freeTextQuery,
                                            judgmentDate = judgmentDate
                                        )
                                    }
                                    return true
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    val activeView = view ?: return
                                    injectPrefillFields(
                                        webView = activeView,
                                        freeText = freeTextQuery,
                                        fromDate = dateRange.first,
                                        toDate = dateRange.second
                                    ) { status ->
                                        if (status.contains("partial", ignoreCase = true) &&
                                            !autoFillWarningShown
                                        ) {
                                            autoFillWarningShown = true
                                            Toast.makeText(
                                                context,
                                                context.getString(R.string.reportable_auto_fill_failed),
                                                Toast.LENGTH_SHORT
                                            ).show()
                                        }
                                    }
                                    startResultsPolling(activeView)
                                    if (!downloadTriggered) {
                                        attemptAutoClickPdf(activeView)
                                    }
                                }
                            }
                            loadUrl(SCI_FREE_TEXT_URL)
                            webViewRef = this
                        }
                    },
                    update = { webViewRef = it }
                )
            } else {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp))
                }
            }
        }
    }

    if (showInfoDialog) {
        AlertDialog(
            onDismissRequest = {},
            title = { Text(text = stringResource(id = R.string.reportable_info_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(text = stringResource(id = R.string.reportable_info_message))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = dontShowAgain,
                            onCheckedChange = { dontShowAgain = it }
                        )
                        Text(text = stringResource(id = R.string.reportable_dont_show_again))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.onFreeTextInfoAcknowledged(dontShowAgain)
                }) {
                    Text(text = stringResource(id = R.string.reportable_continue))
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    viewModel.onFreeTextInfoAcknowledged(dontShowAgain)
                    onBack()
                }) {
                    Text(text = stringResource(id = R.string.reportable_cancel))
                }
            }
        )
    }

    when (val state = downloadState) {
        is ReportableDownloadUiState.Loading -> {
            val hasTotal = state.totalBytes > 0L
            val progress = if (hasTotal) {
                (state.downloadedBytes.toFloat() / state.totalBytes.toFloat()).coerceIn(0f, 1f)
            } else {
                null
            }
            val percent = if (progress != null) (progress * 100f).roundToInt() else null
            AlertDialog(
                onDismissRequest = {},
                title = { Text(text = context.getString(R.string.reportable_judgment_title)) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Text(text = context.getString(R.string.reportable_download_in_progress))
                        if (progress != null) {
                            CircularProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            LinearProgressIndicator(
                                progress = { progress },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = context.getString(
                                    R.string.reportable_downloaded_of_total,
                                    formatBytes(state.downloadedBytes),
                                    formatBytes(state.totalBytes)
                                )
                            )
                            Text(
                                text = context.getString(
                                    R.string.reportable_download_percent,
                                    percent ?: 0
                                )
                            )
                        } else {
                            CircularProgressIndicator(
                                modifier = Modifier.align(Alignment.CenterHorizontally)
                            )
                            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                            Text(
                                text = context.getString(
                                    R.string.reportable_downloaded,
                                    formatBytes(state.downloadedBytes)
                                )
                            )
                        }
                    }
                },
                confirmButton = {}
            )
        }

        is ReportableDownloadUiState.Success -> {
            AnimatedSuccessDialog(
                title = context.getString(R.string.action_success_title),
                message = context.getString(R.string.reportable_download_success),
                confirmText = context.getString(R.string.case_register_ok),
                onConfirm = {
                    downloadTriggered = false
                    viewModel.resetState()
                    onOpenDocuments()
                }
            )
        }

        is ReportableDownloadUiState.Error -> {
            AlertDialog(
                onDismissRequest = {},
                title = { Text(text = context.getString(R.string.reportable_download_failed_title)) },
                text = { Text(text = state.message) },
                confirmButton = {
                    TextButton(onClick = {
                        downloadTriggered = false
                        viewModel.resetState()
                    }) {
                        Text(text = context.getString(android.R.string.ok))
                    }
                }
            )
        }

        else -> Unit
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes < 0L) return "--"
    val kb = 1024.0
    val mb = kb * 1024.0
    val gb = mb * 1024.0
    return when {
        bytes >= gb -> String.format("%.2f GB", bytes / gb)
        bytes >= mb -> String.format("%.2f MB", bytes / mb)
        bytes >= kb -> String.format("%.2f KB", bytes / kb)
        else -> "$bytes B"
    }
}

private class SciResultBridge(
    private val onResultsReady: () -> Unit
) {
    @JavascriptInterface
    fun onResultsReady() {
        onResultsReady()
    }
}

private fun injectPrefillFields(
    webView: WebView,
    freeText: String,
    fromDate: String,
    toDate: String,
    onStatus: (String) -> Unit
) {
    val js = """
        (function() {
            function first(selectors) {
                for (let i = 0; i < selectors.length; i++) {
                    const el = document.querySelector(selectors[i]);
                    if (el) return el;
                }
                return null;
            }
            function setValue(el, value) {
                if (!el) return false;
                el.focus();
                el.value = value;
                el.dispatchEvent(new Event('input', { bubbles: true }));
                el.dispatchEvent(new Event('change', { bubbles: true }));
                return true;
            }
            const freeTextField = first([
                'input[name*="free" i][type="text"]',
                'input[id*="free" i][type="text"]',
                'input[placeholder*="free text" i]',
                'input[name*="search" i][type="text"]',
                'input[id*="search" i][type="text"]'
            ]);
            const fromDateField = first([
                'input[name*="from" i][type="text"]',
                'input[id*="from" i][type="text"]',
                'input[placeholder*="from" i]'
            ]);
            const toDateField = first([
                'input[name*="to" i][type="text"]',
                'input[id*="to" i][type="text"]',
                'input[placeholder*="to" i]'
            ]);
            if (!freeTextField && !fromDateField && !toDateField) return 'skip';
            const freeSet = setValue(freeTextField, '${escapeForJs(freeText)}');
            const fromSet = setValue(fromDateField, '${escapeForJs(fromDate)}');
            const toSet = setValue(toDateField, '${escapeForJs(toDate)}');
            if (freeSet && fromSet && toSet) return 'filled';
            if (freeSet || fromSet || toSet) return 'partial';
            return 'skip';
        })();
    """.trimIndent()
    webView.evaluateJavascript(js) { result ->
        onStatus(result.orEmpty())
    }
}

private fun startResultsPolling(webView: WebView) {
    val js = """
        (function() {
            if (window.__vakilResultsWatcherActive) return;
            window.__vakilResultsWatcherActive = true;
            function hasResults() {
                const tables = Array.from(document.querySelectorAll('table'));
                return tables.some(function(table) {
                    const txt = (table.innerText || '').toLowerCase();
                    return txt.includes('view') || txt.includes('pdf');
                });
            }
            let attempts = 0;
            const timer = setInterval(function() {
                attempts += 1;
                if (hasResults()) {
                    clearInterval(timer);
                    window.__vakilResultsWatcherActive = false;
                    if (window.${JS_BRIDGE_NAME} && window.${JS_BRIDGE_NAME}.onResultsReady) {
                        window.${JS_BRIDGE_NAME}.onResultsReady();
                    }
                } else if (attempts > 120) {
                    clearInterval(timer);
                    window.__vakilResultsWatcherActive = false;
                }
            }, 1000);
        })();
    """.trimIndent()
    webView.evaluateJavascript(js, null)
}

private fun attemptAutoOpenFirstResult(
    webView: WebView,
    onStatus: (String) -> Unit
) {
    val js = """
        (function() {
            function findResultTable() {
                const tables = Array.from(document.querySelectorAll('table'));
                for (let i = 0; i < tables.length; i++) {
                    const txt = (tables[i].innerText || '').toLowerCase();
                    if (txt.includes('view') || txt.includes('pdf')) {
                        return tables[i];
                    }
                }
                return null;
            }
            const table = findResultTable();
            if (!table) return 'no_table';
            const pdf = table.querySelector('a[href$=".pdf"], a[href*=".pdf"], a[href*="Judgement"], a[href*="judgement"], a[href*="supremecourt"]');
            if (pdf) {
                pdf.click();
                return 'clicked_pdf';
            }
            const candidates = Array.from(table.querySelectorAll('a,button,input[type=button],input[type=submit]'));
            const viewAction = candidates.find(function(el) {
                const label = ((el.innerText || el.value || '') + '').trim().toLowerCase();
                return label === 'view' || label.includes('view');
            });
            if (viewAction) {
                viewAction.click();
                return 'clicked_view';
            }
            return 'no_pdf';
        })();
    """.trimIndent()
    webView.evaluateJavascript(js) { result ->
        onStatus(result.orEmpty())
    }
}

private fun attemptAutoClickPdf(webView: WebView) {
    val js = """
        (function() {
            const pdf = document.querySelector('a[href$=".pdf"], a[href*=".pdf"], a[href*="Judgement"], a[href*="judgement"], a[href*="supremecourt"]');
            if (!pdf) return 'no_pdf';
            pdf.click();
            return 'clicked_pdf';
        })();
    """.trimIndent()
    webView.evaluateJavascript(js, null)
}

private fun resolveFreeTextQuery(petitionerName: String?, caseNumber: String?): String {
    val petitioner = petitionerName?.trim().orEmpty()
    if (petitioner.isNotBlank()) return petitioner
    val source = caseNumber?.trim().orEmpty()
    if (source.isBlank()) return ""
    val split = source.split(Regex("\\s+[vV]\\.?\\s+"), limit = 2)
    return split.firstOrNull()?.trim().orEmpty()
}

private fun resolveDateRange(judgmentDate: String?, year: String?): Pair<String, String> {
    val centerDate = parseDate(judgmentDate)
        ?: year?.toIntOrNull()?.let { parsedYear ->
            runCatching { LocalDate.of(parsedYear, 1, 1) }.getOrNull()
        }
        ?: LocalDate.now()
    val formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy")
    return centerDate.minusDays(1).format(formatter) to centerDate.plusDays(1).format(formatter)
}

private fun parseDate(value: String?): LocalDate? {
    if (value.isNullOrBlank()) return null
    val trimmed = value.trim()
    trimmed.toLongOrNull()?.let { epochMillis ->
        return runCatching {
            Instant.ofEpochMilli(epochMillis).atZone(ZoneId.systemDefault()).toLocalDate()
        }.getOrNull()
    }
    val formatters = listOf(
        DateTimeFormatter.ISO_LOCAL_DATE,
        DateTimeFormatter.ofPattern("dd-MM-yyyy"),
        DateTimeFormatter.ofPattern("dd/MM/yyyy")
    )
    formatters.forEach { formatter ->
        val parsed = runCatching { LocalDate.parse(trimmed, formatter) }.getOrNull()
        if (parsed != null) return parsed
    }
    return null
}

private fun isPdfUrl(url: String): Boolean {
    if (url.isBlank()) return false
    val normalized = url.lowercase()
    return normalized.endsWith(".pdf") ||
        normalized.contains(".pdf?") ||
        normalized.contains("judgement_") ||
        (normalized.contains("/supremecourt/") && normalized.contains("judg"))
}

private fun escapeForJs(value: String): String {
    return value
        .replace("\\", "\\\\")
        .replace("'", "\\'")
        .replace("\n", " ")
        .replace("\r", " ")
}

private const val SCI_FREE_TEXT_URL = "https://www.sci.gov.in/free-text-judgements/"
private const val JS_BRIDGE_NAME = "SciResultBridge"
