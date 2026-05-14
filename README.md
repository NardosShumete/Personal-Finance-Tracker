# 💰 Personal Finance Tracker

A modern, robust Android application built with **Kotlin** and **Jetpack Compose**, designed to help users track their daily income and expenses with ease. This project serves as a showcase of mid-to-senior level Android development patterns, including **Clean Architecture**, **Dependency Injection**, **Reactive UI**, and **Localization**.

## 🚀 Features

- **Intuitive Dashboard**: Real-time summary of total balance, income, and expenses.
- **Transaction Management**: Easily add, view, and delete transactions with categories, notes, receipt photos, and dates.
- **Advanced Search & Filter**: Instant, "as-you-type" filtering of transactions by category or notes.
- **Visual Analytics**: Beautiful, animated **Native Canvas Pie Charts** and **Smooth Line Charts** for expense breakdown by category.
- **Biometric Security**: Secure your financial data with fingerprint authentication lock.
- **Multilingual Support**: Fully localized in English and Amharic, with seamless runtime language switching using Jetpack DataStore.
- **User Preferences**: Dark/Light mode toggle, customizable main currency, and monthly goal setting.
- **Data Export**: Export all your transaction history securely to a local CSV file.
- **Offline First**: Fully functional offline storage using the Room Persistence Library.
- **Firebase Authentication**: Email/password sign-in and registration with session persistence.
- **SMS Auto-Parse**: Automatically detects and imports Ethiopian bank transactions from SMS notifications.
- **Receipt Photo Attachment**: Attach and preview receipt images on any transaction.
- **Animated Splash Screen**: Smooth scale + fade-in bounce animation on app launch.
- **Navigation Drawer**: User profile header with username, email, and avatar.
- **Pending SMS Review**: Review, confirm, edit, or dismiss auto-detected transactions before they affect your balance.

## 🛠 Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture
- **Dependency Injection**: [Dagger-Hilt](https://dagger.dev/hilt/)
- **Local Data & Preferences**: [Room](https://developer.android.com/training/data-storage/room) & [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- **Security**: [AndroidX Biometric API](https://developer.android.com/training/sign-in/biometric-auth)
- **Asynchronous Programming**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **Navigation**: [Jetpack Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- **Authentication**: [Firebase Authentication](https://firebase.google.com/docs/auth)
- **Background Processing**: [WorkManager](https://developer.android.com/topic/libraries/architecture/workmanager)
- **Pagination**: [Paging 3](https://developer.android.com/topic/libraries/architecture/paging/v3-overview)
- **Image Loading**: [Coil](https://coil-kt.github.io/coil/)
- **Dependency Management**: Gradle Version Catalog (`libs.versions.toml`)

## 🏗 Architecture Overview

The app follows **Clean Architecture** principles to ensure scalability, maintainability, and testability. It is divided into three main layers:

1. **Data Layer**: Handles data persistence (Room) and preferences (DataStore). Provides implementation for repositories and Mappers to convert between Database Entities and Domain Models.
2. **Domain Layer**: The core business logic. Contains pure Kotlin Models, Repository Interfaces, and **Use Cases (Interactors)** for specific business rules.
3. **UI Layer**: Jetpack Compose screens that observe **UI State (StateFlow)** from ViewModels. ViewModels interact only with Use Cases, keeping them decoupled from data implementation details.

## 🗄 Database Architecture

The app uses **two local storage systems** — no internet or backend required. All data lives on the user's device.

---

### 1. Room Database (`finance_db`) — SQLite

[Room](https://developer.android.com/training/data-storage/room) is Android's official SQLite wrapper. It stores all financial records persistently across app restarts.

**Current schema version: 5**

#### Tables

**`transaction_table`**

| Column | Type | Description |
|---|---|---|
| `id` | INT (PK, auto) | Unique transaction ID |
| `amount` | DOUBLE | Transaction amount (always positive) |
| `category` | TEXT | e.g. `"food"`, `"salary"` |
| `date` | LONG | Unix timestamp (ms) — actual transaction time |
| `type` | TEXT | `"INCOME"` or `"EXPENSE"` |
| `note` | TEXT | User note / description |
| `receiptPath` | TEXT (nullable) | Local path to receipt image |
| `recurringPeriod` | TEXT | `"NONE"`, `"WEEKLY"`, `"MONTHLY"` |
| `source` | TEXT | `"MANUAL"` or `"SMS"` |
| `rawSms` | TEXT (nullable) | Original SMS body (audit trail) |
| `smsBalance` | REAL (nullable) | Running balance from SMS |
| `smsHash` | TEXT (nullable, unique) | SHA-256 deduplication key |
| `smsId` | TEXT (nullable) | Content Provider `_id` for historical sync |
| `isPending` | BOOLEAN | `true` = awaiting user confirmation |

**Indexes:** `date`, `category`, `type`, `source`, `smsHash` (unique)

**`monthly_goal_table`**

| Column | Type | Description |
|---|---|---|
| `monthYear` | TEXT (PK) | Format: `"MM-yyyy"` e.g. `"05-2026"` |
| `incomeGoal` | DOUBLE | Target income for the month |
| `expenseLimit` | DOUBLE | Spending limit for the month |

---

### 2. DataStore Preferences (`settings`)

| Key | Type | Default | Purpose |
|---|---|---|---|
| `biometric_enabled` | Boolean | `false` | Fingerprint lock on/off |
| `is_first_time_user` | Boolean | `true` | Show biometric setup once |
| `is_onboarded` | Boolean | `false` | Show onboarding screens |
| `is_dark_mode` | Boolean? | `null` | Theme override |
| `currency_code` | String | `"ETB"` | Display currency |
| `language_code` | String | `"en"` | App language |
| `user_uid` | String | `""` | Firebase user ID |
| `user_email` | String | `""` | Logged-in email |
| `user_name` | String | `""` | Username |
| `is_logged_in` | Boolean | `false` | Session state |
| `tracked_sms_senders` | String | `""` | Comma-separated bank sender addresses |
| `sms_tracking_enabled` | Boolean | `false` | SMS auto-parse on/off |

---

## 📱 SMS Auto-Parse Feature *(Added: May 14, 2026)*

Automatically detects and imports bank transactions from incoming SMS notifications — no manual entry needed.

### How it works

```
Incoming SMS
  → SmsBroadcastReceiver  (body-based bank detection, < 1ms)
    → WorkManager          (battery-safe, survives process death)
      → SmsProcessWorker   (Hilt-injected, IO thread)
        → ProcessSmsUseCase
          → BankSmsParserFactory  (Strategy Pattern)
            → CbeSmsParser / TelebirrSmsParser / ...
              → AmountParser      (safe, always positive)
              → SmsTimestampParser (actual transaction time)
                → Room INSERT (IGNORE on duplicate)
                  → Flow emits → ViewModel → UI recomposes
```

### Supported Banks

| Bank | Credit keyword | Debit keyword | Balance keyword |
|---|---|---|---|
| CBE | `credited with ETB` | `debited with ETB` | `Available balance: ETB` |
| Dashen | `Credit of ETB` | `Debit of ETB` | `Balance: ETB` |
| Telebirr | `You have received ETB` | `You have sent/paid ETB` | `new balance is ETB` |
| Awash | `Credited ETB` | `Debited ETB` | `Bal: ETB` |
| Abyssinia | `Cr ETB` | `Dr ETB` | `Avail Bal ETB` |

### Parser Architecture — Strategy Pattern

```
core/sms/
  BankSmsParser.kt          ← interface + factory
  ParseResult.kt            ← sealed result (Success / Failure / Ignored)
  SmsParser.kt              ← format detection + routing
  parsers/
    CbeSmsParser.kt
    DashenSmsParser.kt
    TelebirrSmsParser.kt
    AwashSmsParser.kt
    AbyssiniaSmsParser.kt
core/util/
  AmountParser.kt           ← safe parsing, always positive, rejects zero/malformed
  SmsTimestampParser.kt     ← extracts actual transaction time from SMS body
```

**Adding a new bank** = create one file implementing `BankSmsParser` + one line in `BankSmsParserFactory`. Zero changes to existing parsers.

### Production fixes implemented today

| # | Problem | Fix |
|---|---|---|
| 1 | Negative amounts stored in DB | `AmountParser` strips sign, rejects zero/malformed |
| 2 | UI not updating after SMS insert | Room `Flow` → ViewModel `StateFlow` → Compose auto-recompose |
| 3 | Duplicate SMS from telecom | Two-layer dedup: app-level hash check + DB `IGNORE` conflict strategy |
| 4 | Out-of-order SMS used wrong timestamp | `SmsTimestampParser` extracts date from body, falls back to receive time |
| 5 | Regex crash on unexpected SMS format | Every parser wrapped in `try-catch`, returns `ParseResult.Failure` — never crashes |
| 6 | Monolithic parser hard to extend | Strategy Pattern — each bank is an independent, testable class |

### User Account Setup Flow

1. **Settings → SMS Auto-Parse → Enable → Manage Bank Accounts**
2. App scans SMS inbox using **body keywords** (not sender name — fixes false positives)
3. Shows discovered senders with real SMS preview, bank format badge, and message count
4. User selects exactly which accounts to track
5. Exact sender addresses saved to DataStore
6. Historical sync starts in background via `SmsHistorySyncWorker`

### Pending Review Screen

All SMS-parsed transactions appear in **Pending Review** before affecting the balance:
- **Confirm** — accepts the transaction, includes it in totals
- **Edit** — opens an edit sheet to correct amount, category, or type
- **Dismiss** — deletes the pending transaction
- **Confirm All** — bulk-confirms all pending items
- **Sync History** — triggers a fresh historical backfill

### Security & Privacy

- User explicitly selects which senders to track — nothing is automatic
- Bank detection uses SMS **body content**, not sender name (prevents false positives)
- All data stays on-device — nothing sent to any server
- Personal messages are never read or stored
- Duplicate prevention: SHA-256 hash + unique DB index + `IGNORE` conflict strategy

---

## 🎨 UI Redesign *(Updated: May 14, 2026)*

### Midnight FinTech Theme

| Token | Dark Mode | Light Mode | Used for |
|---|---|---|---|
| Background | `#0F172A` (Midnight) | `#F8FAFC` (Near-white) | App canvas |
| Surface | `#1E293B` | `#FFFFFF` | Cards, sheets |
| Primary | `#10B981` (Emerald) | `#059669` (Dark Emerald) | Income, FAB, CTA |
| Expense | `#F43F5E` (Electric Rose) | `#E11D48` (Dark Rose) | Expenses, danger |
| Hero gradient | Blue → Purple → Indigo → Teal | — | Balance card |

### Key UI components

- **Hero Balance Card** — mesh gradient background, animated budget progress bar, frosted glass income/expense pills
- **Transaction Items** — vivid circular category icons (15% opacity background), spring-based press animation
- **Glassmorphism Cards** — `0.75` alpha surface + `0.5dp` white border at `10%` opacity
- **Smooth Line Chart** — cubic Bezier curves, glowing gradient fill, left-to-right draw animation
- **Glowing Donut Chart** — per-arc glow layer, `StrokeCap.Round`, animated draw-on
- **Adaptive theme** — all hardcoded dark colors replaced with `MaterialTheme.colorScheme` tokens; works correctly in both dark and light mode

---

## 🔐 Authentication *(Added: May 14, 2026)*

- Firebase email/password sign-in and registration
- Session persisted in DataStore — skip login on return visits
- Real-time field validation with per-field error messages
- Password strength indicator (5-segment bar + rule checklist)
- Forgot Password — sends Firebase reset email
- `AuthViewModel` exposes `StateFlow<AuthUiState>` — no business logic in Composables

---

## 🗄 How Data Flows

```
UI (Composable)
      ↕  observes StateFlow
  ViewModel
      ↕  calls
  Use Case  ←  validates business rules
      ↕  calls
  Repository Interface
      ↕  implemented by
  RepositoryImpl
      ↕  calls
  DAO (SQL queries)
      ↕
  Room / SQLite  →  finance_db on device storage
```

---

## 📸 Screenshots

<p align="center">
  <img src="screenshots/dashboard.png" width="45%" alt="Dashboard Screenshot" />
  <img src="screenshots/add_transaction.png" width="45%" alt="Add Transaction Screenshot" />
  <img src="screenshots/Analytics.png" width="45%" alt="Analytics Screenshot" />
  <img src="screenshots/Profile.png" width="45%" alt="Profile Screenshot" />
  <img src="screenshots/Setting.png" width="45%" alt="Setting Screenshot" />
</p>

## 🛠 Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/personal-finance-tracker.git
   ```
2. Open the project in **Android Studio (Hedgehog or newer)**.
3. Add your `google-services.json` from Firebase Console into the `app/` folder.
4. Enable **Email/Password** sign-in in Firebase Console → Authentication → Sign-in method.
5. Let Gradle sync and download dependencies.
6. Run the app on an emulator or physical device (API 26+).

> **Note:** Biometric features require a physical device or an emulator configured with fingerprint support. SMS Auto-Parse requires a physical device with a SIM card that receives bank notifications.

---

*Built as a professional portfolio piece to demonstrate modern Android development excellence.*
*Developed by Group 2*

## 🛠 Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture
- **Dependency Injection**: [Dagger-Hilt](https://dagger.dev/hilt/)
- **Local Data & Preferences**: [Room](https://developer.android.com/training/data-storage/room) & [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore)
- **Security**: [AndroidX Biometric API](https://developer.android.com/training/sign-in/biometric-auth)
- **Asynchronous Programming**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **Navigation**: [Jetpack Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- **Dependency Management**: Gradle Version Catalog (`libs.versions.toml`)

## 🏗 Architecture Overview

The app follows **Clean Architecture** principles to ensure scalability, maintainability, and testability. It is divided into three main layers:

1.  **Data Layer**: Handles data persistence (Room) and preferences (DataStore). Provides implementation for repositories and Mappers to convert between Database Entities and Domain Models.
2.  **Domain Layer**: The core business logic. Contains pure Kotlin Models, Repository Interfaces, and **Use Cases (Interactors)** for specific business rules.
3.  **UI Layer**: Jetpack Compose screens that observe **UI State (StateFlow)** from ViewModels. ViewModels interact only with Use Cases, keeping them decoupled from data implementation details.

## � Database Architecture

The app uses **two local storage systems** — no internet or backend required. All data lives on the user's device.

---

### 1. Room Database (`finance_db`) — SQLite

[Room](https://developer.android.com/training/data-storage/room) is Android's official SQLite wrapper. It stores all financial records persistently across app restarts.

**Current schema version: 3**

#### Tables

**`transaction_table`**

| Column | Type | Description |
|---|---|---|
| `id` | INT (PK, auto) | Unique transaction ID |
| `amount` | DOUBLE | Transaction amount |
| `category` | TEXT | e.g. `"food"`, `"salary"` |
| `date` | LONG | Unix timestamp (ms) |
| `type` | TEXT | `"INCOME"` or `"EXPENSE"` |
| `note` | TEXT | User note / description |
| `receiptPath` | TEXT (nullable) | Local path to receipt image |
| `recurringPeriod` | TEXT | `"NONE"`, `"WEEKLY"`, `"MONTHLY"` |

**`monthly_goal_table`**

| Column | Type | Description |
|---|---|---|
| `monthYear` | TEXT (PK) | Format: `"MM-yyyy"` e.g. `"05-2026"` |
| `incomeGoal` | DOUBLE | Target income for the month |
| `expenseLimit` | DOUBLE | Spending limit for the month |

---

### 2. DataStore Preferences (`settings`)

[Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) stores lightweight user preferences as key-value pairs (replaces SharedPreferences).

| Key | Type | Default | Purpose |
|---|---|---|---|
| `biometric_enabled` | Boolean | `false` | Fingerprint lock on/off |
| `is_first_time_user` | Boolean | `true` | Show biometric setup once |
| `is_onboarded` | Boolean | `false` | Show onboarding screens |
| `is_dark_mode` | Boolean? | `null` | Theme override (`null` = follow system) |
| `currency_code` | String | `"ETB"` | Display currency |
| `language_code` | String | `"en"` | App language |

---

### How Data Flows

```
UI (Composable)
      ↕  observes StateFlow
  ViewModel
      ↕  calls
  Use Case  ←  validates business rules
      ↕  calls
  Repository Interface
      ↕  implemented by
  RepositoryImpl
      ↕  calls
  DAO (SQL queries)
      ↕
  Room / SQLite  →  finance_db on device storage
```

Data is read as `Flow<List<...>>` from the DAO, meaning the UI **automatically recomposes** whenever the database changes — no manual refresh needed.

#### Example — saving a transaction

```kotlin
// 1. UseCase validates the input
if (transaction.amount <= 0.0) throw InvalidTransactionException("Amount must be > 0")

// 2. Repository converts domain model → database entity and writes it
dao.insertTransaction(transaction.toEntityModel())

// 3. The Flow in the DAO emits the updated list automatically
// 4. ViewModel collects it → UI recomposes
```

---

### Storage Summary

```
Device Storage
├── finance_db  (SQLite / Room)
│   ├── transaction_table
│   └── monthly_goal_table
└── settings    (DataStore Preferences)
    ├── dark_mode, currency, language
    └── biometric, onboarding flags
```

> **Note:** The database uses `fallbackToDestructiveMigration()`, which means a schema version bump will wipe and recreate all tables. Proper migration scripts should be added before a production release.

---

## �📸 Screenshots

<p align="center">
  <img src="screenshots/dashboard.png" width="45%" alt="Dashboard Screenshot" />
  <img src="screenshots/add_transaction.png" width="45%" alt="Add Transaction Screenshot" />
    <img src="screenshots/Analytics.png" width="45%" alt="Analytics Screenshot" />
  <img src="screenshots/Profile.png" width="45%" alt=" Profile Screenshot" />
  <img src="screenshots/Setting.png" width="45%" alt=" Setting Screenshot" />
</p>


## 🛠 Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/personal-finance-tracker.git
   ```
2. Open the project in **Android Studio (Hedgehog or newer)**.
3. Let Gradle sync and download dependencies.
4. Run the app on an emulator or physical device (API 26+). *Note: Biometric features require testing on a physical device with a fingerprint sensor or an emulator configured with fingerprint support.*

