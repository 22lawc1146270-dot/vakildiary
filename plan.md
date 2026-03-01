## Problem
Implement the requirements from `Ai prompt.md` across three areas: eCourt search, Judgment search, and reportable/free-text PDF download flow, while fitting the current Compose + Hilt + Coroutines architecture.

## Proposed approach
1. **eCourt Search (WebView-assisted detail extraction)**
   - Keep current eCourt search usability but add a visible WebView automation path that:
     - waits for results after user input + captcha,
     - auto-clicks first **View** result,
     - detects full case details page by section headings,
     - extracts sections (Case Details, Case Status, Petitioner/Advocate, Respondent/Advocate, Acts, Case History, Transfer Details) via injected JS JSON payload.
   - Map extracted data into existing `ECourtCaseDetails` model and present in existing detail sheet flow.
   - Preserve explicit user-driven captcha behavior.

2. **Judgment Search (metadata quality + display format)**
   - Update judgment metadata ingestion/search pipeline so results expose petitioner/respondent/citation/year/coram reliably.
   - Align list and preview formatting to the requested legal display style:
     - `{petitioner} v. {respondent} ({citation})`
     - year on next line
     - judge/coram on next line.
   - Keep current download-to-documents behavior intact.

3. **Reportable/Free-text PDF download flow (in-app visible WebView)**
   - Introduce a dedicated visible WebView flow for SCI free-text/reportable retrieval:
     - prefill petitioner + date window (`judgmentDate - 1 day` to `judgmentDate + 1 day`),
     - user manually solves captcha and taps Search,
     - app detects result table and auto-clicks first View/PDF,
     - intercept PDF URL, download via coroutine + OkHttp, and save through existing document attach flow.
   - Add first-use information dialog with “Don’t show again” persisted via DataStore (`UserPreferencesRepository`).
   - Show clear success/failure messaging and graceful fallback when selectors fail.

4. **Navigation and integration wiring**
   - Add/extend route arguments needed for free-text/reportable flow (`caseId`, `petitionerName`, `judgmentDate`, `judgmentId`, optional year/case number).
   - Wire entry points from judgment/documents surfaces without breaking existing reportable action UX.
   - Update EN/HI strings for any new labels/messages.

5. **Validation**
   - Compile-check app module (`:app:assembleDebug`) after implementation.
   - Do not run unit/instrumentation tests unless explicitly requested.

## Todos
- Audit and update eCourt UI/state/repository parser for WebView-driven details extraction.
- Improve SC judgment metadata ingestion and search result formatting.
- Implement SCI free-text/reportable WebView downloader with PDF interception and save.
- Add one-time consent/info dialog persistence in user preferences.
- Wire navigation + entry points + localized strings.
- Run compile validation and fix any integration regressions.

## Notes / considerations
- The workspace currently has pre-existing local changes in some target files; implementation will build on them without reverting unrelated work.
- Captcha must remain user-entered only (no bypass automation).
- Domain layer will remain Android-free; WebView/platform logic stays in presentation/data layers.

## Progress update
- Completed eCourt detail extraction improvements with parser fallback from search HTML.
- Completed judgment metadata/display updates for legal-style listing and preview formatting.
- Completed SCI free-text/reportable visible WebView flow with manual captcha/search, auto View/PDF trigger, PDF interception, and save.
- Completed one-time Supreme Court info dialog persistence using DataStore.
- Completed navigation argument wiring and EN/HI localization updates.
- Validation completed successfully with `:app:compileDebugKotlin` and `:app:assembleDebug`.

## Next steps
- Run manual smoke/regression checks on device for eCourt detail sections and reportable download reliability against live SCI markup changes.
