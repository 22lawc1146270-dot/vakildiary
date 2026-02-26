package com.vakildiary.app.presentation.screens.judgments

import android.webkit.CookieManager
import android.webkit.URLUtil
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vakildiary.app.R
import com.vakildiary.app.presentation.viewmodels.ReportableDownloadUiState
import com.vakildiary.app.presentation.viewmodels.ReportableJudgmentViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportableJudgmentScreen(
    judgmentId: String,
    caseNumber: String?,
    year: String?,
    onBack: () -> Unit,
    viewModel: ReportableJudgmentViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val downloadState by viewModel.downloadState.collectAsStateWithLifecycle()
    val webView = remember { WebView(context) }

    LaunchedEffect(downloadState) {
        if (downloadState is ReportableDownloadUiState.Success) {
            android.widget.Toast.makeText(
                context,
                context.getString(R.string.reportable_download_success),
                android.widget.Toast.LENGTH_SHORT
            ).show()
            viewModel.resetState()
        } else if (downloadState is ReportableDownloadUiState.Error) {
            android.widget.Toast.makeText(
                context,
                (downloadState as ReportableDownloadUiState.Error).message,
                android.widget.Toast.LENGTH_SHORT
            ).show()
            viewModel.resetState()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = context.getString(R.string.reportable_judgment_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(imageVector = Icons.Default.ArrowBack, contentDescription = null)
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    webView.settings.javaScriptEnabled = true
                    webView.settings.domStorageEnabled = true
                    webView.webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView, url: String) {
                            val script = buildAutofillScript(caseNumber, year)
                            view.evaluateJavascript(script, null)
                        }
                    }
                    webView.setDownloadListener { url, userAgent, contentDisposition, mimeType, _ ->
                        val cookies = CookieManager.getInstance().getCookie(url)
                        val fileName = URLUtil.guessFileName(url, contentDisposition, mimeType)
                        viewModel.downloadReportable(
                            url = url,
                            userAgent = userAgent,
                            mimeType = mimeType,
                            cookies = cookies,
                            judgmentId = judgmentId,
                            year = year,
                            caseNumber = caseNumber,
                            fileName = fileName
                        )
                    }
                    webView.loadUrl(SCI_CASE_URL)
                    webView
                }
            )

            if (downloadState is ReportableDownloadUiState.Loading) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }
        }
    }
}

private fun buildAutofillScript(caseNumber: String?, year: String?): String {
    val caseValue = caseNumber?.replace("'", "\\'").orEmpty()
    val yearValue = year?.replace("'", "\\'").orEmpty()
    return """
        (function() {
            function setValueByLabel(labelText, value) {
                if (!value) return false;
                var labels = Array.from(document.querySelectorAll('label'));
                var label = labels.find(function(l) { return l.textContent.toLowerCase().indexOf(labelText) !== -1; });
                if (!label) return false;
                var id = label.getAttribute('for');
                var input = id ? document.getElementById(id) : null;
                if (!input) {
                    input = label.querySelector('input, select') || (label.parentElement && label.parentElement.querySelector('input, select'));
                }
                if (!input) return false;
                input.value = value;
                input.dispatchEvent(new Event('input', { bubbles: true }));
                input.dispatchEvent(new Event('change', { bubbles: true }));
                return true;
            }
            setValueByLabel('case number', '$caseValue');
            setValueByLabel('diary year', '$yearValue') || setValueByLabel('year', '$yearValue');
        })();
    """.trimIndent()
}

private const val SCI_CASE_URL = "https://www.sci.gov.in/judgements-case-no/"
