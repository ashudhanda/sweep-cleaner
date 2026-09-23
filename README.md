# Sweep — Smart Storage & Duplicate Cleaner for Android

Sweep is a modern, privacy-focused Android cleaner application built with Jetpack Compose, Material Design 3, Room, WorkManager, and the Android Storage Access Framework (SAF).

## ✨ Features

- 🧹 **Smart Scan**: One-tap analysis of junk caches, residual files, obsolete APKs, and duplicate media.
- 📁 **SAF Deep Storage Scanner**:
  - Recursively scans any directory or external SD card via Android's Storage Access Framework (`DocumentsContract`).
  - Identifies large files (25 MB+), archives, ISOs, and raw video files.
  - Multi-tier duplicate detection using size partitioning, fast partial chunk hashing, and full SHA-256 cryptographic verification.
  - Intelligent keeper recommendations (safely keep the original copy).
- 🖼️ **Similar Photos & Burst Analysis**: Groups camera shots taken within tight temporal windows and perceptual similarity with a side-by-side comparison slider.
- 📱 **Screenshots Review**: Filter, review, and mass-clean old screen captures.
- 🎥 **Large Videos Manager**: Sort and stream video clips with duration and bitrate indicators.
- 👥 **Duplicate Contacts Cleaner**: Non-destructive contact merging with safe field preview.
- 🗑️ **30-Day Trash Quarantine**:
  - All cleaned items are moved to a secure quarantine folder with automated 30-day retention.
  - Full restore-to-original capability or instant permanent purge.
- ⏰ **Scheduled Background Maintenance**: Automated periodic maintenance via Android `WorkManager` with battery-friendly constraints.
- 🎨 **Material 3 & Dark Mode**: Dynamic theming, custom adaptive launcher icon, and accessible 48dp+ interactive targets.

## 🛠️ Tech Stack & Architecture

- **Language**: 100% Kotlin
- **UI Toolkit**: Jetpack Compose with Material 3 Design
- **Architecture**: Clean MVVM (Model-View-ViewModel) with StateFlow
- **Data Persistence**: Room Database (`SweepDatabase` with Room KSP)
- **Preferences**: Jetpack DataStore
- **Background Tasks**: Android Jetpack WorkManager
- **Storage System**: Android MediaStore & Storage Access Framework (SAF)
- **Build System**: Gradle Kotlin DSL (`build.gradle.kts`) with Version Catalog (`libs.versions.toml`)

## 🚀 Building and Running

1. Clone this repository:
   ```bash
   git clone https://github.com/<your-username>/sweep-cleaner.git
   ```
2. Open the project in **Android Studio Hedgehog** or newer.
3. Allow Gradle to sync dependencies.
4. Run on an Android device or emulator running **Android 9.0 (API 28)** or higher.

## 📄 License

This project is licensed under the Apache License 2.0.
