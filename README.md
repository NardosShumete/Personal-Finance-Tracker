# 💰 Personal Finance Tracker

A modern, robust Android application built with **Kotlin** and **Jetpack Compose**, designed to help users track their daily income and expenses with ease. This project follows **Clean Architecture** and is strictly **Offline First**.

## 🚀 Features

- **Intuitive Dashboard**: Real-time summary of total balance, income, and expenses.
- **Calendar & Reminders**: Schedule and get notified for deposits, withdrawals, and recurring bills like house rent (yebet kiray).
- **SMS Auto-Parse**: Automatically detects and imports Ethiopian bank transactions from SMS notifications.
- **Biometric Security**: Secure your financial data with fingerprint authentication lock.
- **Local Authentication**: Secure email/password login handled entirely on-device (No Firebase/Backend needed).
- **Visual Analytics**: Native Canvas Pie Charts and Smooth Line Charts for expense breakdown.
- **Multilingual Support**: Fully localized in English and Amharic.
- **Transaction Management**: Add notes, categories, and receipt photos to any record.
- **Data Export**: Export your history to a local CSV file.

## 🏗 Architecture Overview

The app follows **Clean Architecture** principles:
1. **Data Layer**: Room (SQLite) for persistence and DataStore for preferences.
2. **Domain Layer**: Pure Kotlin business logic and Use Cases.
3. **UI Layer**: Jetpack Compose screens observing StateFlow from ViewModels.

## 🗄 Database Architecture

### 1. Room Database (`finance_db`)
- `transaction_table`: Stores all financial records.
- `monthly_goal_table`: Stores budget targets.
- `reminder_table`: Stores scheduled notifications for rent, deposits, etc.

### 2. DataStore Preferences
Stores app settings, biometric flags, and local user session data.

## 🛠 Tech Stack

- **UI**: Jetpack Compose (Material 3)
- **DI**: Dagger-Hilt
- **Async**: Coroutines & Flow
- **Background**: WorkManager (for SMS parsing and Reminder notifications)
- **Local DB**: Room

## 🛠 Installation

1. Clone the repository.
2. Open in **Android Studio**.
3. Let Gradle sync.
4. Run the app — **No extra configuration (like Firebase) is required.**

---
*Built as a professional portfolio piece. Developed by Group 2.*
