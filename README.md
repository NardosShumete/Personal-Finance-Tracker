# 💰 Personal Finance Tracker

A modern, robust Android application built with **Kotlin** and **Jetpack Compose**, designed to help users track their daily income and expenses with ease. This project serves as a showcase of mid-to-senior level Android development patterns, including **Clean Architecture**, **Dependency Injection**, **Reactive UI**, and **Localization**.

## 🚀 Features

- **Intuitive Dashboard**: Real-time summary of total balance, income, and expenses.
- **Transaction Management**: Easily add, view, and delete transactions with categories, notes, receipt photos, and dates.
- **Advanced Search & Filter**: Instant, "as-you-type" filtering of transactions by category or notes.
- **Visual Analytics**: Beautiful, animated **Native Canvas Pie Charts** for expense breakdown by category.
- **Biometric Security**: Secure your financial data with fingerprint authentication lock.
- **Multilingual Support**: Fully localized in English and Amharic, with seamless runtime language switching using Jetpack DataStore.
- **User Preferences**: Dark/Light mode toggle, customizable main currency, and monthly goal setting.
- **Data Export**: Export all your transaction history securely to a local CSV file.
- **Offline First**: Fully functional offline storage using the Room Persistence Library.

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

## 📸 Screenshots

<p align="center">
  <img src="screenshots/dashboard.png" width="45%" alt="Dashboard Screenshot" />
  <img src="screenshots/add_transaction.png" width="45%" alt="Add Transaction Screenshot" />
</p>

> **Note:** To display these beautiful screenshots, make sure to save the two pictures you just uploaded into the `screenshots` folder as `dashboard.png` and `add_transaction.png` respectively!

## 🛠 Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/personal-finance-tracker.git
   ```
2. Open the project in **Android Studio (Hedgehog or newer)**.
3. Let Gradle sync and download dependencies.
4. Run the app on an emulator or physical device (API 26+). *Note: Biometric features require testing on a physical device with a fingerprint sensor or an emulator configured with fingerprint support.*

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).

---
*Built as a professional portfolio piece to demonstrate modern Android development excellence.*
