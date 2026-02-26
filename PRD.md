
VakilDiary
Litigation Case Management System
Product Requirements Document  —  v1.1

Document Version	1.0 — Initial Release
Date	February 2026
Platform	Android (iOS planned — Phase 2)
Architecture	Kotlin • Jetpack Compose • Clean Architecture
Target Users	Advocates & Lawyers (Tech-Savvy)
Auth Model	Google Sign-In + Google Drive Backup
Language	English + Hindi (Bilingual UI)

The Digital Replacement for the Advocate's Executive Diary

1. App Overview
VakilDiary is a complete litigation case management Android application designed to digitally replace the physical executive calendar diary used by tech-savvy advocates and lawyers in India. The app enables legal professionals to manage all aspects of active cases including case registration, hearing scheduling, task management, fee tracking, document storage, and eCourt integration — all within a single, offline-first mobile application with Google Drive backup.

1.1 Core Value Proposition
    • Completely replaces the physical advocate's diary with a structured digital system
    • Full offline functionality — works in court halls without internet
    • eCourt integration to check live case status without leaving the app
    • Supreme Court judgment downloads directly within the app
    • Smart calendar with colour-coded hearing dates and task deadlines
    • Google Drive delta sync — data is always backed up and restorable

1.2 Target Users
User Type	Description
Primary	Individual advocates managing their own practice (1 user, up to 3 devices)
Secondary	Tech-savvy junior advocates or legal assistants supporting a senior
Future (Phase 2)	Law firm teams with shared case databases (multi-user mode)

1.3 Platform & Deployment
Attribute	Value
Phase 1 Platform	Android 8.0 (API 26) and above
Phase 2 Platform	iOS 15 and above (shared KMP business logic)
Distribution	Google Play Store
Architecture Approach	Kotlin Multiplatform ready from Day 1 (domain layer must be pure Kotlin, no Android imports)
Minimum RAM	2 GB
Storage	Minimum 100 MB free space for documents

2. Technology Stack
All technology choices below are non-negotiable and must be enforced via AGENTS.md. The AI coding agent must never use deprecated libraries (AsyncTask, RxJava, XML Layouts, Java).

2.1 Core Stack
Layer	Technology	Version	Purpose
Language	Kotlin	2.0+	Primary language. No Java.
UI Framework	Jetpack Compose	1.6+	All UI. No XML layouts.
Architecture	MVVM + Clean Architecture	—	Presentation / Domain / Data
DI	Hilt	2.51+	All dependency injection
Async	Coroutines + Flow	1.8+	All async ops. No RxJava.
Local DB	Room	2.6+	All structured local data
Navigation	Compose Navigation	2.7+	Type-safe nav. No Fragments.
Network	Retrofit + OkHttp	2.11+	eCourt + SC API calls
Image Loading	Coil	2.6+	Profile pics, doc thumbnails
Preferences	DataStore (Proto)	1.1+	User settings & theme tokens
Auth	Google Sign-In (Credential Manager)	1.2+	Google account auth
Drive Backup	Google Drive API v3	—	Delta sync & backup
PDF Generation	iTextPDF / PdfDocument	5.x	Case summary & fee export
Doc Scanner	ML Kit Document Scanner	16.0+	Camera document scanning
File Handling	Android FileProvider	—	All file types support
Notifications	WorkManager + AlarmManager	2.9+	Scheduled reminders
Testing	JUnit5 + Mockk + Compose Test	—	Unit + UI tests

2.2 External APIs & Integrations
API / Service	Purpose & Repository
OpenJustice eCourt API	Case status lookup (github.com/openjustice-in) — Read-only
Indian SC Judgments API	Download SC judgments (github.com/vanga/indian-supreme-court-judgments)
Google Drive API v3	Delta backup & restore of all app data
Google Sign-In (OAuth2)	User authentication — no password stored on device
ML Kit Document Scanner	In-app camera document scanning to PDF

2.3 AGENTS.md System Prompt (Copy into project root)
# VakilDiary AGENTS.md
You are an expert Senior Android Developer for VakilDiary.
## ABSOLUTE RULES:
- Use ONLY Kotlin. Never Java.
- Use ONLY Jetpack Compose. Never XML layouts.
- Use ONLY Hilt for DI. Never manual injection.
- Use ONLY Coroutines + Flow. Never RxJava/AsyncTask.
- Domain layer must have ZERO Android imports (KMP ready).
- Always read PRD.md before generating any feature.
- Wrap ALL network/DB calls in Result<T> sealed class.
- UI state must be a sealed interface: Loading/Success/Error.
- Never invent library functions. Check libs.versions.toml first.

3. User Stories
3.1 Case Management
    1. As an advocate, I want to add a new case with name, number, court, client, type, and stage so that I can start tracking it immediately.
    2. As an advocate, I want to view a list of all my active cases sorted by next hearing date so that I can prioritise my day.
    3. As an advocate, I want to tap on a case name to view full case details including all fields and upcoming tasks.
    4. As an advocate, I want to view the complete history of a case showing all past hearing dates and notes from each hearing.
    5. As an advocate, I want to update the current stage of a case (Filing / Hearing / Arguments / Judgment / Disposed) after each hearing.
    6. As an advocate, I want to search cases by case name, case number, client name, or opposite party name.

3.2 Hearing & Calendar Management
    7. As an advocate, I want to add a next hearing date to any case so that it appears on my calendar.
    8. As an advocate, I want to view a monthly calendar where every hearing date is marked with a blue dot and every task deadline with an orange dot.
    9. As an advocate, I want to tap on a calendar date and see a list of all hearings and tasks scheduled for that day.
    10. As an advocate, I want to record what happened in a hearing (notes, order, adjournment reason) so that the case history is preserved.

3.3 Task Management
    11. As an advocate, I want to add tasks to a specific case (e.g., File petition, Collect papers, View ordersheet, Get photocopies) with a deadline.
    12. As an advocate, I want to mark tasks as complete and see pending vs completed tasks per case.
    13. As an advocate, I want to see all overdue tasks across all cases in a dedicated urgent view.

3.4 Client & Meeting Management
    14. As an advocate, I want to add client meetings with date, time, location, and notes against a specific case.
    15. As an advocate, I want to view all upcoming client meetings in a chronological list.

3.5 Fees Management
    16. As an advocate, I want to set the total agreed fees for a case.
    17. As an advocate, I want to record each payment received from a client with date, amount, and payment mode (cash/UPI/cheque).
    18. As an advocate, I want to see the outstanding balance and payment history for any case at a glance.
    19. As an advocate, I want to export a fee ledger for a specific case as a PDF to share with my client.

3.6 Document Manager
    20. As an advocate, I want to attach any file type (PDF, image, Word, Excel, audio, video) to a specific case.
    21. As an advocate, I want to scan a physical document using my phone camera and save it as a PDF against a case.
    22. As an advocate, I want to view, rename, and delete documents attached to a case.
    23. As an advocate, I want to search for a document by name across all cases.

3.7 eCourt Integration
    24. As an advocate, I want to search for a case on eCourt by case number and court without leaving the app.
    25. As an advocate, I want to import a case found on eCourt directly into my VakilDiary case list.
    26. As an advocate, I want hearing dates found on eCourt to be automatically synced to my calendar.
    27. As an advocate, I want to receive a notification when eCourt updates the status of any of my tracked cases.

3.8 Supreme Court Judgments
    28. As an advocate, I want to search for and download any Supreme Court judgment directly within the app.
    29. As an advocate, I want to save downloaded judgments to a specific case's document folder.

3.9 Backup & Sync
    30. As an advocate, I want to back up all my case data, documents, and settings to Google Drive with a single tap.
    31. As an advocate, I want delta sync so that only changed data is uploaded, saving mobile data.
    32. As an advocate, I want to restore all my data on a new device by signing in with the same Google account.

3.10 Notifications
    33. As an advocate, I want a notification every morning listing all hearings and tasks scheduled for today.
    34. As an advocate, I want to set a custom reminder (X days or hours before) for any specific hearing or task deadline.
    35. As an advocate, I want to receive a notification when an eCourt-tracked case status changes.

4. Data Schema
4.1 Core Entities (Room Database)

4.1.1 Case Entity
Field	Type	Required	Description
caseId	String (UUID)	YES	Primary key, auto-generated
caseName	String	YES	Descriptive name of the case
caseNumber	String	YES	Official court case number
courtName	String	YES	Name of the court
courtType	Enum	YES	DISTRICT / HIGH / SUPREME / TRIBUNAL
clientName	String	YES	Primary client full name
clientPhone	String	NO	Client mobile number
clientEmail	String	NO	Client email address
oppositeParty	String	NO	Opposite party name
caseType	Enum	YES	CIVIL / CRIMINAL / WRIT / APPEAL / REVISION / OTHER
caseStage	Enum	YES	FILING / HEARING / ARGUMENTS / JUDGMENT / DISPOSED
assignedJudge	String	NO	Presiding judge name
firNumber	String	NO	FIR number (criminal cases only)
actsAndSections	String	NO	Relevant acts and sections (comma separated)
nextHearingDate	Long (epoch)	NO	Timestamp of next scheduled hearing
totalAgreedFees	Double	NO	Total fees agreed with client
isECourtTracked	Boolean	YES	Whether eCourt auto-sync is active
eCourtCaseId	String	NO	Case ID on eCourt for API polling
notes	String	NO	General notes about the case
createdAt	Long (epoch)	YES	Record creation timestamp
updatedAt	Long (epoch)	YES	Last modified timestamp
isArchived	Boolean	YES	Soft delete flag (default false)

4.1.2 HearingHistory Entity
Field	Type & Description
hearingId	String (UUID) — Primary key
caseId	String — Foreign key to Case
hearingDate	Long (epoch) — Date of the hearing
purpose	String — Purpose of the hearing (e.g., Arguments)
outcome	String — What happened (Order passed / Adjourned / etc.)
orderDetails	String — Full text of the order passed
nextDateGiven	Long (epoch) — Next date assigned during this hearing
adjournmentReason	String — Reason if adjourned
createdAt	Long (epoch) — Record creation timestamp

4.1.3 Task Entity
Field	Type & Description
taskId	String (UUID) — Primary key
caseId	String — Foreign key to Case
title	String — Task description (e.g., File petition)
taskType	Enum — FILE_PETITION / COLLECT_PAPERS / VIEW_ORDERSHEET / PHOTOCOPY / MEETING / CUSTOM
deadline	Long (epoch) — Task deadline timestamp
reminderMinutesBefore	Int — Custom reminder offset in minutes
isCompleted	Boolean — Completion status
completedAt	Long (epoch) — Timestamp when completed
notes	String — Additional notes about the task
createdAt	Long (epoch) — Record creation timestamp

4.1.4 Payment Entity
Field	Type & Description
paymentId	String (UUID) — Primary key
caseId	String — Foreign key to Case
amount	Double — Amount paid in this transaction
paymentDate	Long (epoch) — Date of payment
paymentMode	Enum — CASH / UPI / CHEQUE / BANK_TRANSFER / OTHER
referenceNumber	String — UPI/cheque/transaction ref (optional)
receiptPath	String — Local file path to receipt image (optional)
notes	String — Additional payment notes
createdAt	Long (epoch) — Record creation timestamp

4.1.5 Document Entity
Field	Type & Description
documentId	String (UUID) — Primary key
caseId	String — Foreign key to Case (nullable for orphan docs)
fileName	String — Display name of the document
filePath	String — Absolute local path in app's private storage
fileType	String — MIME type (application/pdf, image/jpeg, etc.)
fileSizeBytes	Long — File size in bytes
isScanned	Boolean — Whether document was scanned via camera
thumbnailPath	String — Path to generated thumbnail (optional)
tags	String — Comma-separated tags for search
createdAt	Long (epoch) — Record creation timestamp

4.1.6 Meeting Entity
Field	Type & Description
meetingId	String (UUID) — Primary key
caseId	String — Foreign key to Case
clientName	String — Client name (pre-filled from case)
meetingDate	Long (epoch) — Scheduled date and time
location	String — Meeting location or 'Phone Call'
agenda	String — Purpose of meeting
notes	String — Post-meeting notes
reminderMinutesBefore	Int — Custom reminder offset
createdAt	Long (epoch) — Record creation timestamp

5. Feature Specifications
5.1 Case Management Module
    • Add Case screen with mandatory fields: Case Name, Case Number, Court Name, Court Type, Client Name, Case Type, Case Stage
    • Optional fields: Opposite Party, Judge Name, FIR Number, Acts & Sections, Client Phone, Client Email, Total Agreed Fees, Notes
    • Case List screen with sorting: Next Hearing (default), Case Name A-Z, Recently Added, Court
    • Filter panel: filter by Court Type, Case Type, Case Stage
    • Search bar with real-time filtering across Case Name, Number, Client, Opposite Party
    • Case Detail screen: full case info, next hearing chip, quick action buttons (Add Hearing, Add Task, Add Payment, Add Document)
    • Case History tab: timeline of all past hearings with date, outcome, and order details
    • Edit and delete case with choice of archive (soft delete) or permanent delete (removes related data)

5.2 Hearing & Calendar Module
    • Add Hearing Date screen: select case, date/time, purpose, set custom reminder
    • Calendar screen: monthly view with coloured dots (Blue = hearing, Orange = task, Purple = meeting)
    • Tap on date: show day agenda list with all events for that date
    • Record Hearing Outcome: open a dialog to add outcome, order details, next date, adjournment reason — auto-appends to case history
    • Weekly agenda view as alternative to monthly calendar

5.3 Task Management Module
    • Add Task screen: select case, task type (predefined + custom), title, deadline, custom reminder
    • Predefined task types: File Petition, Collect Papers from Court, View Ordersheet, Get Photocopies, Pay Court Fees, Prepare Arguments, Custom
    • Tasks screen: tabs for Pending, Completed, Overdue
    • Overdue tasks badge on app bottom navigation icon
    • Swipe to complete / swipe to delete gesture on task list

5.4 Fees Management Module
    • Fees summary card on Case Detail: Agreed / Received / Outstanding shown as a progress bar
    • Add Payment screen: amount, date, payment mode, reference number, attach receipt photo
    • Payment History list: all transactions for the case sorted by date
    • Export Fee Ledger as PDF: includes case details, agreed fees, payment table, outstanding balance
    • WhatsApp share of fee summary to client with one tap

5.5 Document Manager Module
    • Attach file from device storage: supports all MIME types (PDF, images, DOC, XLS, audio, video, etc.)
    • Scan Document via camera using ML Kit Document Scanner — saves as PDF to case
    • Document list per case: thumbnail for images, file icon for others, file name, size, date
    • Tap to open document in system viewer (FileProvider Intent)
    • Long press: rename, delete, move to another case, share via intent
    • Global document search by file name across all cases
    • Document storage in app-private directory, encrypted at rest

5.6 eCourt Integration Module
Powered by OpenJustice eCourt API (github.com/openjustice-in). All eCourt data is read-only.
    • eCourt Search screen: search by Court Type, State, District, Case Number, Year
    • Search results show: case title, parties, next hearing date, current stage
    • Import Case button: pre-fills Add Case form with eCourt data for confirmation
    • Auto-sync hearing dates: when eCourt tracking is enabled for a case, WorkManager polls eCourt periodically and updates the calendar
    • eCourt Status Change Notification: push notification when case status changes on eCourt
    • Offline mode: eCourt search gracefully degrades with 'No internet connection' message

5.7 Supreme Court Judgments Module
Powered by indian-supreme-court-judgments API (github.com/vanga/indian-supreme-court-judgments).
    • Judgment Search screen: search by case name, citation, year, bench, or keyword
    • Search results list: case name, date, bench, citation number
    • Tap result: preview judgment details (metadata)
    • Download Judgment button: downloads PDF to device and optionally attaches to a case
    • Offline: downloaded judgments are stored locally and accessible without internet

5.8 Backup & Google Drive Sync Module
    • Google Sign-In on first launch using Credential Manager API — required for Drive backup
    • Manual Backup Now button in Settings: creates a backup of all Room DB + documents + settings
    • Delta Sync: only files changed since last backup are uploaded (tracked via checksum)
    • Backup includes: full Room database export (SQLite), all attached documents, app settings JSON
    • Auto-backup schedule: user-configurable (daily / weekly / manual only)
    • Restore: on new device, sign in with same Google account and tap Restore from Drive
    • Backup status screen: shows last backup time, size, sync log

5.9 Notification System
    • Daily Morning Digest (8:00 AM): notification listing all hearings and task deadlines for today
    • Custom Reminder: advocate sets X hours/days before any hearing or task — delivered by WorkManager
    • eCourt Status Alert: notification when a tracked case status changes (polled by background WorkManager job)
    • Notification channels (Android 8+): Hearing Reminders / Task Reminders / eCourt Alerts / Daily Digest
    • Tap notification: deep-links directly to the relevant case or task in the app
    • All notification times respect the device timezone

5.10 Sharing & Export Module
    • Share Case Summary as PDF: generates a formatted 1-page PDF of case details
    • Export Full Case History as PDF: complete timeline of all hearings and outcomes
    • WhatsApp Share of Hearing Date: pre-formatted message 'Dear [Client], Your next hearing in [Case] is on [Date] at [Court]. Regards, Adv. [Name]'
    • All sharing uses Android Share Sheet (system intent) — user chooses target app

6. UI/UX Guidelines
6.1 Design System
Property	Specification
Design Language	Material Design 3 (Material You)
Theme System	Token-based with system default + manual override
Default Mode	Follows system (light in day / dark at night)
Manual Override	User can lock to Light or Dark in Settings
Primary Color	#1A3C5E (Deep Navy Blue — represents authority/law)
Secondary Color	#2E75B6 (Professional Blue)
Tertiary/Accent	#E67E22 (Amber — for urgent/overdue items)
Success Color	#1E7A4A (Green — for completed/paid items)
Error Color	#C0392B (Red — for errors and overdue)
Typography — Display	Noto Serif (legal/professional feel for headers)
Typography — Body	Noto Sans (clean readability for data)
Typography — Mono	Noto Sans Mono (case numbers, legal codes)
Corner Radius	12dp (cards), 8dp (chips), 50% (FABs)
Elevation	Material 3 tonal elevation (no hard shadows)

6.2 Navigation Structure
Bottom Nav Tab	Content
Home (Dashboard)	Today's hearings + tasks + overdue count + quick add FAB
Cases	Searchable, filterable case list + Add Case FAB
Calendar	Monthly calendar view with coloured dots + day agenda
Documents	Global document manager across all cases
More / Settings	Profile, Backup, Theme, Notifications, About

6.3 Key Screen Specifications
    • Dashboard: Card for Today's Hearings, Card for Today's Tasks, Card for Overdue items, Quick Stats strip (Total cases / Pending fees / Upcoming in 7 days)
    • Case Detail: Tabbed layout — Overview / History / Tasks / Fees / Documents / Meetings
    • Calendar: Standard month grid. Dots below dates. Day panel slides up on tap. Week view toggle.
    • Add Case: Single scrollable form. Mandatory fields marked with asterisk. Inline validation.
    • eCourt Search: Dropdown for court type/state/district + case number input. Results in bottom sheet.

6.4 Hindi Localisation
    • All UI strings must have Hindi translations in strings.xml (hi resource qualifier)
    • Legal terms use standard Hindi equivalents: Vakeel (Advocate), Muqadma (Case), Sunwai (Hearing), Tihi (Date), Adalat (Court)
    • User can switch language in Settings without restarting the app (Activity recreation)
    • Date formats follow Indian standard: DD/MM/YYYY
    • Currency format: Indian Rupee symbol ₹ with Indian number formatting (lakhs/crores)

7. Project Architecture & Directory Structure
7.1 Clean Architecture Layers
Package	Contents & Responsibility
com.vakildiary.data	Room Entities, DAOs, RepositoryImpl classes, Remote API services (Retrofit), DTOs, Google Drive sync manager
com.vakildiary.domain	Domain Models (pure Kotlin), Repository Interfaces, UseCases. ZERO Android imports. KMP-ready.
com.vakildiary.presentation	Compose Screens, ViewModels, UI State sealed interfaces, Navigation graph
com.vakildiary.di	Hilt Modules: DatabaseModule, NetworkModule, RepositoryModule, UseCaseModule
com.vakildiary.core	Shared utilities: DateUtils, FileUtils, Result<T>, Resource<T>, Constants, Extensions
com.vakildiary.notifications	WorkManager Workers for reminders and eCourt polling
com.vakildiary.backup	Google Drive backup/restore logic, delta sync manager, checksum calculator

7.2 State Management Pattern
All ViewModels expose a sealed interface UiState with exactly three states. The Kotlin compiler enforces completeness in the UI layer.
sealed interface CaseListUiState {
    object Loading : CaseListUiState
    data class Success(val cases: List<Case>) : CaseListUiState
    data class Error(val message: String) : CaseListUiState
}

7.3 Build Execution Order (Vertical Slicing)
Follow this exact sequence. Never build UI before the data layer is complete and tested.
    36. Room Database: All Entities + DAOs + Database class
    37. Hilt Modules: DatabaseModule providing all DAOs
    38. Domain Models: Pure Kotlin data classes mirroring entities
    39. Repository Interfaces: in domain layer
    40. Repository Implementations: in data layer, map entities to domain models
    41. UseCases: one action per UseCase class
    42. ViewModels: inject UseCases, expose StateFlow<UiState>
    43. Compose Screens: observe ViewModel state, render UI
    44. Navigation Graph: wire all screens
    45. Integration Tests: test each ViewModel with mocked repository

8. Edge Cases & Error Handling
8.1 Offline State
    • All core features (case management, calendar, tasks, fees, documents) must work 100% offline
    • eCourt search and SC Judgment download: show a persistent banner 'No internet connection. eCourt features unavailable.' with a Retry button
    • Drive backup: queue backup request and execute when connectivity is restored via WorkManager NetworkConstraint

8.2 Empty States
    • Case List empty: illustration of a diary with 'No cases yet. Add your first case.' and a prominent Add Case button
    • Calendar date with no events: 'No hearings or tasks on this date'
    • Document list empty: 'No documents attached to this case. Tap + to add.'
    • Payment history empty: 'No payments recorded yet.'

8.3 Data Validation
    • Case Number: must not be empty; warn but do not block if format does not match standard pattern
    • Date fields: past dates allowed for recording historical hearings; future dates required for new hearing scheduling
    • Fees: amount must be a positive number; payment amount cannot exceed outstanding balance (show warning, not block)
    • Duplicate case check: warn user if a case with the same case number already exists

8.4 Permission Handling
    • Camera permission: required for document scanning — show rationale dialog before requesting
    • Notification permission (Android 13+): request on first launch with clear explanation
    • Storage permission: use scoped storage (no WRITE_EXTERNAL_STORAGE for API 29+)
    • Drive permission: request only when user initiates first backup
    • All permissions: graceful degradation if denied — feature is disabled with 'Grant permission' chip visible

8.5 Large File Handling
    • Document upload: files larger than 50 MB show a warning dialog before attachment
    • Video files: always compressed before attaching if over 100 MB
    • Drive backup: if total backup size exceeds available Drive space, show storage warning and allow selective backup

8B. Today's Docket — Bottom Sheet Feature
This feature is the digital equivalent of an advocate flipping open their diary to today's page. It provides instant access to all hearings and tasks scheduled for the current day from anywhere in the app, without navigating away from the current screen.

8B.1 Feature Overview
Attribute	Specification
UI Pattern	Modal Bottom Sheet (Material Design 3)
Trigger — Primary	Persistent floating action button (FAB) visible on all screens
Trigger — Secondary	Programmatic open from Today's hearing/task notification tap
FAB Position	Bottom-right, 16dp above the bottom navigation bar
FAB Icon	Gavel / scales icon with a badge showing count of pending items today
FAB Color	Amber (#E67E22) — distinct from the primary blue to signal urgency
Sheet Height	60% of screen height, expandable to 90% by dragging up
Dismiss	Swipe down, tap scrim, or tap close button — all dismiss the sheet
Date Scope	Always shows today's date only. Not navigable to other dates.
Data Source	Queries HearingHistory + Task tables filtered by today's date
Empty State	Shows encouraging message: 'No hearings or tasks today. Enjoy the day!'

8B.2 Bottom Sheet Content Structure
The sheet is divided into two clearly labelled sections. Hearings always appear first, tasks second.

8B.2.1 Sheet Header (fixed, does not scroll)
    • Drag handle pill — 40dp wide, 4dp tall, centered at top
    • Title: 'Today's Docket' in Playfair Display / Noto Serif font
    • Subtitle: Current date in format 'Monday, 17 February 2026'
    • Close button (X) — top right corner
    • Progress chip: '2 of 5 done' — updates in real time as items are checked

8B.2.2 Hearings Section
    • Section label: 'HEARINGS (N)' where N is the count — shown even if 0
    • Each hearing item shows: checkbox + case name (bold) + court name + scheduled time + 'Hearing' chip (blue)
    • Items sorted by scheduled hearing time (earliest first)
    • Completed hearings show strikethrough on case name and greyed checkbox

8B.2.3 Tasks Section
    • Section label: 'TASKS (N)' where N is the count — shown even if 0
    • Each task item shows: checkbox + task title (bold) + case name + task type + 'Task' chip (amber)
    • Overdue tasks (past deadline, not completed) shown with red accent
    • Completed tasks show strikethrough on task title and green checkbox

8B.3 Interaction Flows

8B.3.1 Tapping a Task Checkbox
    46. User taps the checkbox next to a task item
    47. Checkbox animates to green checked state immediately (optimistic UI update)
    48. Task title gets strikethrough styling
    49. Task is marked completed in Room DB (isCompleted = true, completedAt = now)
    50. Progress chip in sheet header updates count
    51. No dialogue required — tasks are simple completion toggles
    52. Tapping a completed task's checkbox again un-completes it (toggle behaviour)

8B.3.2 Tapping a Hearing Checkbox — The Outcome Dialogue
This is the most important interaction. When a hearing is marked complete, the advocate must record what happened for the case history.

    53. User taps the checkbox next to a hearing item
    54. Hearing Outcome Dialogue opens immediately as a dialog overlaid on the bottom sheet
    55. Dialogue title shows: 'Record Hearing Outcome' + case name in blue subtitle

The dialogue contains two fields:

    • Field 1 — 'What happened today?' (mandatory label, optional to fill)
        ◦ A multiline text input (3 rows) for the advocate to type a short note
        ◦ Below the text input: a 'Add voice note instead' button (dashed border, microphone icon)
        ◦ Tapping voice note button opens Android's native voice recorder intent
        ◦ Voice recording is saved as an audio file and linked to the HearingHistory record

    • Field 2 — 'Next hearing date?' (optional)
        ◦ A date picker input field in DD/MM/YYYY format
        ◦ Pre-filled placeholder: 'Tap to select date'
        ◦ If left empty: case's nextHearingDate remains unchanged
        ◦ If a date is entered: case's nextHearingDate is updated automatically

Dialogue action buttons:
    • 'Skip & Mark Done' (secondary button) — saves outcome as empty, marks hearing complete, closes dialogue
    • 'Save & Mark Done' (primary navy button) — saves the note/voice + next date, marks hearing complete, closes dialogue

    56. On save (either button): HearingHistory record is created with hearingDate = today, outcome = text note, voiceNotePath = file path if recorded, nextDateGiven = date if provided
    57. Case's nextHearingDate is updated in the Case table if a date was provided
    58. Hearing checkbox animates to green checked state
    59. Case name shows strikethrough in the docket list
    60. Progress chip updates
    61. Dialogue closes, user is back on the bottom sheet

8B.3.3 Updating Case Details After the Fact
If the advocate dismisses the dialogue without entering a next date (or taps 'Skip'), they can update the case later from the Case Detail screen:
    • Case Detail screen → tap the 'Next Hearing' chip → inline date picker opens
    • Case Detail screen → Edit button → full edit form with nextHearingDate, courtName, caseStage fields
    • This is documented in user-facing help text as: 'You can always update the next hearing date from the case detail screen'

8B.4 FAB Badge Behaviour
    • Badge shows count of incomplete hearings + incomplete tasks for today
    • Badge is hidden (no badge) when count is 0
    • Badge updates in real time as items are checked off
    • Badge uses red background (#C0392B) with white text for maximum visibility
    • When all items for today are completed: FAB changes to green with a ✓ icon

8B.5 Data Changes Required
Entity	Change Required
HearingHistory	Add voiceNotePath: String? field for voice note file path
HearingHistory	Existing outcome: String field stores the typed note text
HearingHistory	Existing nextDateGiven: Long? field stores the next date if provided
Case	nextHearingDate: Long? is updated when dialogue provides a new date
Task	Existing isCompleted + completedAt fields are sufficient — no changes needed

8B.6 New DAO Queries Required
// CaseDao — get all cases with hearings today
@Query("SELECT * FROM cases WHERE DATE(nextHearingDate/1000,'unixepoch') = DATE('now') AND isArchived = 0")
fun getCasesWithHearingToday(): Flow<List<CaseEntity>>

// TaskDao — get all incomplete tasks due today
@Query("SELECT * FROM tasks WHERE DATE(deadline/1000,'unixepoch') = DATE('now') AND isCompleted = 0")
fun getTasksDueToday(): Flow<List<TaskEntity>>

8B.7 ViewModel Specification
// TodayDocketViewModel
data class DocketItem(
    val id: String,
    val title: String,       // case name or task title
    val subtitle: String,    // court+time or case name
    val type: DocketType,    // HEARING or TASK
    val isCompleted: Boolean
)

sealed interface DocketUiState {
    object Loading : DocketUiState
    data class Success(
        val hearings: List<DocketItem>,
        val tasks: List<DocketItem>,
        val completedCount: Int,
        val totalCount: Int
    ) : DocketUiState
    data class Error(val message: String) : DocketUiState
}

8B.8 Gemini CLI Build Prompt for This Feature
Use this exact prompt when you reach this feature in Phase 1 of the build:
Context: Read PRD.md Section 8B completely. AGENTS.md rules apply.
CaseDao, TaskDao, HearingDao, and all repositories already exist.

Task: Implement the Today's Docket Bottom Sheet feature.

Step 1: Add getCasesWithHearingToday() to CaseDao.
Step 2: Add getTasksDueToday() to TaskDao.
Step 3: Add voiceNotePath field to HearingHistoryEntity.
Step 4: Create DocketItem data class and DocketUiState sealed interface.
Step 5: Create TodayDocketViewModel exposing StateFlow<DocketUiState>.
Step 6: Create TodayDocketBottomSheet composable with:
  - Drag handle, header with date and progress chip
  - Hearings section with DocketItemRow composables
  - Tasks section with DocketItemRow composables
  - Animated checkbox (unchecked -> green checked)
  - Strikethrough text animation on completion
Step 7: Create HearingOutcomeDialog composable with:
  - Case name subtitle
  - Multiline text field for outcome note
  - Voice note button triggering audio record intent
  - Date picker field for next hearing date
  - 'Skip & Mark Done' and 'Save & Mark Done' buttons
Step 8: Add amber FAB with badge to MainScaffold.
  - FAB badge count = incomplete hearings + tasks today
  - FAB turns green with checkmark when all done

Constraints: Material Design 3. Jetpack Compose only.
Use ModalBottomSheet from compose-material3.
Wrap all DB operations in Result<T>.
Do Step 1 and 2 only first. Show me the code before continuing.

9. Deployment & Play Store Requirements
9.1 App Identity
Property	Value
App Name	VakilDiary — Case Manager
App Name (Hindi)	वकील डायरी
Package Name	com.vakildiary.app
Min SDK	API 26 (Android 8.0 Oreo)
Target SDK	API 35 (Android 15)
Build Type	Release — Signed AAB (Android App Bundle)
Version (Initial)	1.0.0 (versionCode: 1)
Category	Productivity / Business
Content Rating	Everyone

9.2 Permissions Required (AndroidManifest.xml)
    • INTERNET — eCourt API, Drive sync, SC Judgment download
    • CAMERA — ML Kit document scanner
    • POST_NOTIFICATIONS — Hearing and task reminders (Android 13+)
    • RECEIVE_BOOT_COMPLETED — Restart WorkManager alarms on device reboot
    • USE_BIOMETRIC — Optional app lock feature
    • SCHEDULE_EXACT_ALARM — Precise notification delivery

9.3 Privacy Policy Requirements
Required by Google Play. Must disclose:
    • Google Sign-In: user's Google account email is accessed for Drive authentication
    • Camera: used only for document scanning; images stored locally, not uploaded to any server except user's own Google Drive
    • No analytics or third-party advertising SDKs
    • All case data is stored locally on device and user's personal Google Drive only
    • GDPR + India PDPB 2023 compliant

9.4 Build Prompt for AI Agent
# Gemini CLI Deployment Prompt
Read PRD.md and AGENTS.md.
Review AndroidManifest.xml and all Gradle files.
Generate a Privacy Policy compliant with GDPR
and India PDPB 2023 disclosing all permissions
and third-party services used.
Then walk me through generating a Signed AAB
in Android Studio for Play Store submission.

10. Build Roadmap — Phased Delivery
Phase 1 — Core MVP (Build First)
    62. Project setup: Gradle, libs.versions.toml, AGENTS.md, PRD.md in root
    63. Room Database: all 6 entities + DAOs
    64. Hilt DI setup: all modules
    65. Case CRUD: Add, View, Edit, Archive case
    66. Hearing management: add hearing date, record outcome, case history
    67. Task management: add, complete, delete tasks with overdue tracking
    68. Calendar screen: monthly view with coloured dots
    69. Dashboard / Home screen
    70. Bottom navigation and type-safe navigation graph
    71. Today's Docket Bottom Sheet: FAB + sheet + hearing outcome dialogue + voice note

Phase 2 — Data & Finance
    72. Fees management: add payments, fee ledger, outstanding balance
    73. Document manager: attach files, view, delete
    74. ML Kit document scanner integration
    75. PDF export: case summary, fee ledger
    76. WhatsApp share of hearing dates

Phase 3 — Integrations & Sync
    77. Google Sign-In with Credential Manager
    78. Google Drive backup and restore
    79. Delta sync engine
    80. eCourt API integration (OpenJustice)
    81. Supreme Court Judgment search and download

Phase 4 — Polish & Deploy
    82. Notification system: WorkManager workers for all reminder types
    83. Hindi localisation: full strings.xml translation
    84. Token-based theme system: light/dark/system with Material You dynamic colour
    85. App lock with biometrics
    86. Play Store listing: screenshots, description, privacy policy
    87. Signed AAB build and submission

End of VakilDiary Product Requirements Document v1.1
Prepared with AI assistance • February 2026 • Feed this document to your AI agent before every coding session
