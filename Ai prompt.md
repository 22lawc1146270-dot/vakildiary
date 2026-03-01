You can do this entirely on‑device with WebView: let the user fill everything and solve captcha, programmatically click **View**, then parse the details section by section and show them in your own UI. [services.ecourts.gov](https://services.ecourts.gov.in/ecourtindia_v6/?p=casestatus%2Findex)

## 1. Auto‑click the View button

After the search results list loads (with the “View” link/button), run JS once to trigger the first result’s View:

```kotlin
override fun onPageFinished(view: WebView?, url: String?) {
    // Detect that you are on the list page (after Go)
    if (url?.contains("casestatus") == true) {
        val js = """
            (function() {
                // Try common patterns – adjust after inspecting HTML:
                // 1) A button or link with text 'View'
                let btn = Array.from(document.querySelectorAll('a, button, input[type=button]'))
                    .find(el => el.innerText.trim() === 'View' || el.value === 'View');
                if (btn) { btn.click(); return 'clicked'; }
                return 'not_found';
            })();
        """.trimIndent()

        webView.evaluateJavascript(js, null)
    }
}
```

Use Chrome remote debugging to inspect the exact tag/id/class of the View button and replace the `find` logic with a precise selector like:

```javascript
document.querySelector('input[value="View"]')?.click();
```

## 2. Detect the full case‑details page

When the View click finishes loading, the page shows sections like “Case Details”, “Case Status”, “Petitioner and Advocate”, etc., similar to the official eCourts app. [play.google](https://play.google.com/store/apps/details?id=in.gov.ecourts.eCourtsServices&hl=en_IN)
In `onPageFinished`, detect this page with either:

- URL pattern (sometimes extra query params), or  
- Presence of headings containing those captions in the DOM:

```kotlin
override fun onPageFinished(view: WebView?, url: String?) {
    val js = """
        (function() {
            const txt = document.body.innerText;
            if (txt.includes('Case Details') && txt.includes('Case Status') && txt.includes('Petitioner and Advocate')) {
                return 'case_details';
            }
            return 'other';
        })();
    """.trimIndent()

    webView.evaluateJavascript(js) { result ->
        if (result?.contains("case_details") == true) {
            extractCaseDetails()
        }
    }
}
```

## 3. Extract all required sections

Write one JS block that returns a big JSON object with all captions:

```kotlin
private fun extractCaseDetails() {
    val js = """
        (function() {
            const data = {};

            function textAfter(label) {
                const el = Array.from(document.querySelectorAll('td, th, div, span'))
                    .find(e => e.innerText.trim() === label);
                if (!el) return '';
                const td = el.parentElement?.querySelector('td:nth-child(2)');
                if (td) return td.innerText.trim();
                // fallback: next sibling
                if (el.nextElementSibling) return el.nextElementSibling.innerText.trim();
                return '';
            }

            // 1) Case Details
            data.caseDetails = {
                caseType: textAfter('Case Type'),
                filingNumber: textAfter('Filing Number'),
                filingDate: textAfter('Filing Date'),
                registrationNumber: textAfter('Registration Number'),
                registrationDate: textAfter('Registration Date'),
                cnr: textAfter('CNR Number')
            };

            // 2) Case Status
            data.caseStatus = {
                firstHearingDate: textAfter('First Hearing Date'),
                nextHearingDate: textAfter('Next Hearing Date'),
                stateOfCase: textAfter('Case Status'),
                courtNumber: textAfter('Court Number'),
                judge: textAfter('Judge')
            };

            // 3) Petitioner and Advocate
            data.petitionerAndAdvocate = textAfter('Petitioner and Advocate');

            // 4) Respondent and Advocate
            data.respondentAndAdvocate = textAfter('Respondent and Advocate');

            // 5) Acts
            data.acts = textAfter('Acts');

            // 6) Case History (may be a table)
            const historyTable = Array.from(document.querySelectorAll('table'))
                .find(t => t.innerText.includes('Date') && t.innerText.includes('Proceeding'));
            if (historyTable) {
                const rows = Array.from(historyTable.querySelectorAll('tr')).slice(1);
                data.caseHistory = rows.map(tr => {
                    const tds = tr.querySelectorAll('td');
                    return {
                        date: tds[0]?.innerText.trim() || '',
                        proceeding: tds [services.ecourts.gov](https://services.ecourts.gov.in/ecourtindia_v6/?p=casestatus%2Findex)?.innerText.trim() || '',
                        purpose: tds [play.google](https://play.google.com/store/apps/details?id=in.gov.ecourts.eCourtsServices&hl=en_IN)?.innerText.trim() || ''
                    };
                });
            } else {
                data.caseHistory = [];
            }

            // 7) Case Transfer Details
            data.transferDetails = textAfter('Transfer Details');

            return JSON.stringify(data);
        })();
    """.trimIndent()

    webView.evaluateJavascript(js) { json ->
        if (json != null && json != "null") {
            val root = JSONObject(json)

            val caseDetails = root.getJSONObject("caseDetails")
            val caseStatus = root.getJSONObject("caseStatus")
            val petitionerAndAdv = root.getString("petitionerAndAdvocate")
            val respondentAndAdv = root.getString("respondentAndAdvocate")
            val acts = root.getString("acts")
            val history = root.getJSONArray("caseHistory")
            val transfer = root.getString("transferDetails")

            // Map to your data models and save/display in Room + UI
        }
    }
}
```

You will need to adjust the `textAfter('Label')` function once you inspect the real HTML around each caption (they might be in separate tables or divs).

## 4. Data flow in your app

| Step                         | Happens where       | Your role in code                            |
|------------------------------|---------------------|----------------------------------------------|
| User fills form + captcha    | WebView UI          | No automation, just load URL and show page. |
| User taps “Go”               | WebView             | Nothing extra.                              |
| App auto‑clicks “View”       | JS in WebView       | Run small `btn.click()` JS once.            |
| Case details page loads      | WebView             | Detect via headings and call extractor.     |
| Extract 1–7 sections         | JS → JSON → Kotlin  | Map JSON to models, store in Room, show UI. |

If you can share a small copied HTML around any one caption (e.g., “Case Details” heading and the table below it), I can turn the generic `textAfter` helper into exact `querySelector` calls that match the real structure.











Use an Athena query over the Parquet metadata, then format the result in your app.

### 1. Athena table (from docs)

First create the external table as given in the repo, if you have not already:

```sql
CREATE DATABASE supreme_court_cases;

CREATE EXTERNAL TABLE supreme_court_cases.judgments (
  title STRING,
  petitioner STRING,
  respondent STRING,
  description STRING,
  judge STRING,
  author_judge STRING,
  citation STRING,
  case_id STRING,
  cnr STRING,
  decision_date STRING,
  disposal_nature STRING,
  court STRING,
  available_languages STRING,
  raw_html STRING,
  path STRING,
  nc_display STRING,
  scraped_at STRING
)
PARTITIONED BY (year STRING)
STORED AS PARQUET
LOCATION 's3://indian-supreme-court-judgments/metadata/parquet/'
TBLPROPERTIES (
  'has_encrypted_data'='false',
  'projection.enabled'='true',
  'projection.year.type'='integer',
  'projection.year.range'='1950,2025',
  'storage.location.template'='s3://indian-supreme-court-judgments/metadata/parquet/year=${year}/'
);
```  


### 2. Query for a given year in the format you want

For “all judgments for a selected year” with the exact fields you need:

```sql
SELECT
  petitioner,
  respondent,
  citation,
  year,
  judge
FROM
  supreme_court_cases.judgments
WHERE
  year = '2023'
ORDER BY
  decision_date;
```  

This gives you, per row:

- petitioner  
- respondent  
- citation (for “(case citation)”)  
- year  
- judge (list of judges; you can label it as “coram” in UI).  



### 3. How to render in the app

For each row returned:

```text
1) {petitioner} V. {respondent} ({citation})
    {year}
    {judge}
    (coram)
```

In code (pseudo-Kotlin):

```kotlin
val display = "${petitioner} V. ${respondent} (${citation})\n" +
              "    $year\n" +
              "    $judge\n" +
              "    (coram)"
```

If you later need decision_date as a separate line, just include it in the SELECT.









***

You are an expert Android developer (Kotlin, AndroidX, WebView, coroutines, Room).

Context  
I am building a legal research Android app.  
I already have complete case data from my AWS backend, including:

- `petitionerName` (String)  
- `judgmentDate` (LocalDate or yyyy-MM-dd String, e.g. 2026-01-10)  
- `caseId` (local DB primary key)  
- other metadata (party names, citation, etc.)

I now want to integrate the Supreme Court of India **Free Text Judgments** page inside my app:

- URL: https://www.sci.gov.in/free-text-judgements/ [sci.gov](https://www.sci.gov.in/free-text-judgements/)

Overall goal  
From my case detail screen, user taps a button “Download judgment (Free Text)”.  
Then this flow should happen:

1. App opens the SCI Free Text page inside the app (WebView in a dedicated Activity/Fragment). [sci.gov](https://www.sci.gov.in/free-text-judgements/)
2. App automatically fills:
   - The “Free Text” input box with the petitioner name.
   - The “From Date” and “To Date” fields as:
     - From Date = judgmentDate minus one day (e.g. if judgment date is 10 Jan 2026, from date = 09 Jan 2026).
     - To Date = judgmentDate plus one day (e.g. 11 Jan 2026).  
3. User manually solves the CAPTCHA (this must not be automated in any way). [sci.gov](https://www.sci.gov.in/free-text-judgements/)
4. User manually presses the SCI “Search” button.  
5. SCI displays the search results table for that date range and free text query, with one or more rows each containing a “View” or equivalent action and a “PDF” link for the judgment. [sci.gov](https://www.sci.gov.in/free-text-judgements/)
6. As soon as the results are loaded:
   - The app automatically triggers the first result’s “View” action (or directly the “PDF” link if “View” is not required).
7. Once the “View” opens and a “PDF” link is available:
   - The app automatically clicks/activates the PDF link.
   - Instead of opening the PDF in the WebView, the app intercepts that request and downloads the PDF.
   - The PDF is saved into app-private storage and linked to the current case in my local database.

Important constraints (legal / UX)  
1. CAPTCHA must always be solved manually by the user. Never attempt to bypass, solve, or pre-fill it. [sci.gov](https://www.sci.gov.in/free-text-judgements/)
2. The user must explicitly press the SCI “Search” button on the Free Text page.  
3. After results load, it is acceptable for the app to programmatically click the “View” and “PDF” elements as a convenience.  
4. All interaction happens in a visible WebView. No hidden/background scraping of the SCI website. [sci.gov](https://www.sci.gov.in/free-text-judgements/)

Implementation details I want

A. Host screen

- Create `FreeTextJudgmentActivity` (or Fragment) which:
  - Receives via Intent/arguments: `caseId`, `petitionerName`, `judgmentDate` (yyyy-MM-dd or millis), and any other useful metadata.  
  - Displays a WebView that loads `https://www.sci.gov.in/free-text-judgements/`. [sci.gov](https://www.sci.gov.in/free-text-judgements/)

B. WebView setup

- Use AndroidX WebView.  
- Enable JavaScript.  
- Add a custom `WebViewClient` and, if needed, `WebChromeClient`.  
- Once the Free Text page finishes loading (e.g. in `onPageFinished`), inject JavaScript that:
  1. Locates the “Free Text” input field and sets its value to `petitionerName`.  
  2. Computes `fromDate` = judgmentDate minus 1 day, `toDate` = judgmentDate plus 1 day on the Kotlin side, formats them in the exact date format expected by the SCI page (look at the page; it uses DD-MM-YYYY / DD/MM/YYYY etc.). [sci.gov](https://www.sci.gov.in/free-text-judgements/)
  3. Sets the “From Date” and “To Date” fields accordingly with JS.  
- Do **not** touch or reference any CAPTCHA elements in JS.

C. Detecting when results are loaded

- After user solves CAPTCHA and presses “Search”, SCI renders a results table. [sci.gov](https://www.sci.gov.in/free-text-judgements/)
- Implement a mechanism to detect when that results table appears, for example:
  - Use `onPageFinished` again for the results URL, or  
  - Inject polling JavaScript that periodically checks for the presence of the results table (by id/class) and, once found, calls a custom JS interface method.

- Define a `@JavascriptInterface` Kotlin class, e.g. `SciResultBridge`, that the JS can call when results are ready.

D. Automatically clicking View and PDF

- Once results are detected:
  - Use injected JavaScript to:
    - Find the first row in the results table.
    - Programmatically “click” the “View” element (if there is a separate View button/link).  
    - After the View screen loads and displays a “PDF” link, programmatically click that “PDF” link.  
- If the results page already has direct “PDF” links in the list, you can skip the View step and click the first PDF link directly. [sci.gov](https://www.sci.gov.in/free-text-judgements/)

E. Intercepting PDF and saving it

- In the `WebViewClient`, override `shouldOverrideUrlLoading` and/or `shouldInterceptRequest` to identify PDF URLs:
  - Check for URL ending with `.pdf` or containing patterns similar to SCI judgment PDFs (e.g. `/supremecourt/` and `Judgement_` in the path). [sci.gov](https://www.sci.gov.in/free-text-judgements/)
  - When such a URL is detected:
    - Prevent the WebView from navigating to it.
    - Start a coroutine (e.g. in `lifecycleScope`) that:
      - Uses `OkHttp` (preferred) or `HttpURLConnection` to download the PDF.
      - Streams it to a file in internal storage, e.g. `/files/judgments/` with a file name like `<caseId>_<yyyyMMdd>.pdf`.
      - On success, updates my Room database:
        - Assume DAO: `caseDao.updateJudgmentPath(caseId: Long, pdfPath: String)` and field `hasJudgmentPdf: Boolean`.
      - On completion, show a toast/snackbar:  
        - Success: “Judgment downloaded and saved.”  
        - Failure: “Judgment download failed, please try again.”  

F. One-time info dialog

- On first time this feature is used, before showing the WebView:
  - Show a dialog:
    - Title: “Supreme Court website”
    - Message (write in your own words, short):  
      - Explain that the app opens the official Supreme Court of India Free Text Judgments page inside a WebView,  
      - The user must solve the CAPTCHA and press Search,  
      - The app then helps automatically open and save the PDF for their convenience. [sci.gov](https://www.sci.gov.in/free-text-judgements/)
    - Buttons: “Continue” and “Cancel”.
    - Checkbox: “Don’t show again”. Use SharedPreferences or DataStore to remember this flag.
  - If user cancels, close the activity. If user continues, proceed to load the WebView.

G. Error handling

- If the SCI page markup changes (e.g. JS cannot find fields or result table), handle gracefully:
  - Show a toast/snackbar: “Automatic filling failed, please search manually.” and let WebView behave like a normal browser.  
- If no PDF link is found after search, show: “No judgment PDF available for this query.”  
- Log errors with clear tags so I can debug.

What I need from you

1. Full Kotlin code for:
   - `FreeTextJudgmentActivity` (or Fragment) with WebView setup.  
   - JS injection to fill Free Text and date fields.  
   - Result detection (JS + `@JavascriptInterface`).  
   - Automatic clicking of first result’s View/PDF link.  
   - PDF interception and download with OkHttp.  
   - Room DAO update call (`caseDao.updateJudgmentPath`).  
2. Minimal data and DAO stubs (`CaseEntity`, `CaseDao`) to make the example clear.  
3. One-time info dialog implementation with SharedPreferences/DataStore.  
4. Necessary manifest entries (INTERNET permission, Activity declaration).  
5. Clear comments around:
   - Where CAPTCHA is intentionally left fully user-driven,  
   - Where JS is modifying the DOM,  
   - Where the PDF is intercepted and saved.

---