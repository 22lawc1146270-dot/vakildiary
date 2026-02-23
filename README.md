# VakilDiary — Litigation Case Management System

![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-7F52FF?style=flat-square&logo=kotlin)
![Compose](https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=flat-square&logo=jetpack-compose)
![Architecture](https://img.shields.io/badge/Architecture-Clean%20%2B%20MVVM-orange?style=flat-square)

**VakilDiary** is a complete, offline-first litigation case management system designed to digitally replace the physical executive calendar diary used by advocates and lawyers in India. Built with a modern Android stack, it enables legal professionals to manage cases, hearings, tasks, fees, and documents seamlessly.

---

## 🚀 Key Features

### ⚖️ Case Management
- Full CRUD operations for legal cases.
- Track case details: number, court name, parties, stage (Filing/Hearing/Judgment), and acts/sections.
- Search and filter cases by name, number, or client.

### 📅 Smart Calendar & Docket
- Monthly calendar with color-coded markers for hearings (Blue), tasks (Orange), and meetings (Purple).
- **Today's Docket:** A quick-access bottom sheet to view and manage all of today's legal obligations.
- Automated hearing history tracking with outcome recording.

### 📝 Task & Client Management
- Link tasks and meetings directly to specific cases.
- Set deadlines and receive automated reminders via WorkManager.
- Track client details and contact information.

### 💰 Finance & Fee Tracker
- Record agreed fees and track payments (Cash/UPI/Cheque).
- Generate and export professional **Fee Ledgers as PDF**.
- Real-time outstanding balance tracking.

### 📄 Document Manager & Scanner
- Attach files (PDF, Images, Docs) to specific cases.
- Built-in **ML Kit Document Scanner** to convert physical papers into PDFs.
- Secure, app-private storage.

### 🔍 Legal Integrations
- **eCourt Search:** Search and track cases directly via eCourt API integration.
- **SC Judgments:** Search and download Supreme Court judgments within the app.

### ☁️ Backup & Sync
- **Google Drive Delta Sync:** Automated and manual backups to your personal Google Drive.
- Secure authentication via Google Sign-In (Credential Manager).
- Cross-device data restoration.

---

## 🛠 Tech Stack

- **Language:** [Kotlin 2.0+](https://kotlinlang.org/)
- **UI:** [Jetpack Compose](https://developer.android.com/jetpack/compose) (100% Declarative UI)
- **Architecture:** Clean Architecture (Domain, Data, Presentation) + MVVM
- **Dependency Injection:** [Hilt](https://developer.android.com/training/dependency-injection/hilt-android)
- **Database:** [Room](https://developer.android.com/training/data-storage/room) (Offline-first)
- **Networking:** [Retrofit](https://square.github.io/retrofit/) + OkHttp
- **Background Tasks:** [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- **Image Loading:** [Coil](https://coil-kt.github.io/coil/)
- **PDF Generation:** [iTextPDF](https://itextpdf.com/)
- **Local Settings:** [DataStore (Proto)](https://developer.android.com/topic/libraries/architecture/datastore)

---

## 🏗 Project Structure

```text
app/src/main/java/com/vakildiary/app/
├── core/               # Shared utilities, Result wrappers, Extensions
├── data/               # Room Entities, DAOs, Repository Impl, Remote APIs
├── di/                 # Hilt Modules (Database, Network, Repository)
├── domain/             # Domain Models, Repository Interfaces, UseCases (KMP Ready)
├── notifications/      # WorkManager Workers & Notification Channels
├── presentation/       # Compose Screens, ViewModels, Theme, Navigation
└── security/           # Biometric Lock Manager
```

---

## 🏁 Getting Started

### Prerequisites
- **JDK 17**
- **Android Studio Ladybug (2024.2.1)** or newer
- **Android SDK 35** (Target) / **API 26** (Minimum)

### Installation
1.  **Clone the repository:**
    ```bash
    git clone https://github.com/your-username/vakildiary.git
    cd vakildiary
    ```

2.  **Open in Android Studio:**
    - File > Open > Select the `vakildiary` folder.
    - Wait for Gradle sync to complete.

3.  **Setup API Keys (Optional for local build):**
    - To use Google Drive Backup, you will need to add your `google-services.json` to the `app/` directory and configure your OAuth2 client ID in the Google Cloud Console.

### Building the App
To build a debug APK:
```bash
./gradlew assembleDebug
```

To build a **64-bit Release APK** (arm64-v8a):
```bash
./gradlew assembleRelease
```
*Note: Ensure you have configured signing configs in `build.gradle.kts` for release builds.*

To generate an **Android App Bundle (AAB)** for Play Store:
```bash
./gradlew bundleRelease
```

---

## 🌍 Localization
VakilDiary fully supports **Bilingual UI**:
- **English** (Default)
- **Hindi (हिन्दी)**
Localization is handled via `res/values/strings.xml` and `res/values-hi/strings.xml`.

---

## 🤝 Contributing
1. Fork the Project.
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`).
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`).
4. Push to the Branch (`git push origin feature/AmazingFeature`).
5. Open a Pull Request.

---

## 📄 License
Distributed under the MIT License. See `LICENSE` for more information.

---

## 📧 Contact
Project Link: [https://github.com/your-username/vakildiary](https://github.com/your-username/vakildiary)
