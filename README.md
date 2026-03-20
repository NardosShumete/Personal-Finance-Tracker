# 💰 Personal Finance Tracker

A modern, robust Android application built with **Kotlin** and **Jetpack Compose**, designed to help users track their daily income and expenses with ease. This project serves as a showcase of mid-to-senior level Android development patterns, including **Clean Architecture**, **Dependency Injection**, and **Reactive UI**.

## 🚀 Features

- **Intuitive Dashboard**: Real-time summary of total balance, income, and expenses (formatted in **Ethiopian Birr - Br**).
- **Transaction Management**: Easily add, view, and delete transactions with categories, notes, and dates.
- **Advanced Search & Filter**: Instant, "as-you-type" filtering of transactions by category or notes.
- **Visual Analytics**: Beautiful, animated **Native Canvas Pie Charts** for expense breakdown by category.
- **Modern User Interface**: Built entirely with Jetpack Compose and Material 3, supporting both **Dark and Light themes**.
- **Offline First**: Fully functional offline storage using the Room Persistence Library.

## 🛠 Tech Stack

- **Language**: [Kotlin](https://kotlinlang.org/)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (Material 3)
- **Architecture**: MVVM (Model-View-ViewModel) + Clean Architecture
- **Dependency Injection**: [Dagger-Hilt](https://dagger.dev/hilt/)
- **Local Database**: [Room](https://developer.android.com/training/data-storage/room)
- **Asynchronous Programming**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & [Flow](https://kotlinlang.org/docs/flow.html)
- **Navigation**: [Jetpack Compose Navigation](https://developer.android.com/jetpack/compose/navigation)
- **Dependency Management**: Gradle Version Catalog (`libs.versions.toml`)
- **Code Generation**: KSP (Kotlin Symbol Processing)

## 🏗 Architecture Overview

The app follows **Clean Architecture** principles to ensure scalability, maintainability, and testability. It is divided into three main layers:

1.  **Data Layer**: Handles data persistence (Room) and provides implementation for repositories. Includes Mappers to convert between Database Entities and Domain Models.
2.  **Domain Layer**: The core business logic. Contains pure Kotlin Models, Repository Interfaces, and **Use Cases (Interactors)** for specific business rules (e.g., amount validation).
3.  **UI Layer**: Jetpack Compose screens that observe **UI State (StateFlow)** from ViewModels. ViewModels interact only with Use Cases, keeping them decoupled from data implementation details.

## 📸 Screenshots

*(Tip: Add your own screenshots here to make your portfolio pop!)*

## 🛠 Installation

1. Clone the repository:
   ```bash
   git clone https://github.com/yourusername/personal-finance-tracker.git
   ```
2. Open the project in **Android Studio (Hedgehog or newer)**.
3. Let Gradle sync and download dependencies.
4. Run the app on an emulator or physical device (API 26+).

## 📄 License

This project is open-source and available under the [MIT License](LICENSE).

---
*Built as a professional portfolio piece to demonstrate modern Android development excellence.*
