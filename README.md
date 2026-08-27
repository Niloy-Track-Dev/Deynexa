# Daynexa

<div align="center">

<h3>Build Better Days.</h3>

<p>A privacy-first, fully offline Android productivity and daily routine tracker built with modern Jetpack Compose and Room.</p>

[![Android CI/CD](https://github.com/niloymitra/daynexa/actions/workflows/android.yml/badge.svg)](https://github.com/niloymitra/daynexa/actions/workflows/android.yml)
[![Latest Release](https://img.shields.io/github/v/release/niloymitra/daynexa?color=blue&label=Release)](https://github.com/niloymitra/daynexa/releases)
[![License: MIT](https://img.shields.io/badge/License-MIT-emerald.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%208.0%2B%20%28API%2026%2B%29-brightgreen.svg)](https://android.com)
[![Offline-First](https://img.shields.io/badge/Architecture-100%25%20Offline--First-blueviolet.svg)](#offline-architecture)
[![Zero Telemetry](https://img.shields.io/badge/Telemetry-Zero%20Tracking-black.svg)](#privacy)

</div>

---

## Overview

**Daynexa** is a lightweight, distraction-free productivity application designed to help you construct and sustain meaningful daily routines. Unlike traditional productivity tools that require cloud accounts, bombard you with notifications, or monetize your activity data, Daynexa operates **100% locally on your device**.

With deterministic scheduling, an interactive monthly calendar matrix, visual streak analytics, a 14-week consistency heatmap, and complete JSON backup export/import, Daynexa gives you total control over your habits without sacrificing your privacy.

---

## Features

### 📅 Routine & Habit Tracking
- **Deterministic Scheduling**: Configure one-off tasks or recurring routines on specific days of the week (Every Day, Weekdays, Weekends, or custom selections).
- **Flexible Time Windows**: Support for scheduled time ranges with Material 3 time pickers, or all-day flexible habits.
- **Task State Progression**: Effortlessly toggle between **Pending**, **Completed**, and **Skipped** states with responsive haptic feedback and smooth animations.

### 📊 Visual Analytics & Consistency Heatmap
- **Productivity Dashboard**: Monitor key performance metrics including Current Daily Streak, Completion Rate, Peak Productive Day, and Total Completed Routines.
- **14-Week Consistency Heatmap**: GitHub-style activity grid visualizing daily habit execution over time with 4-level color intensity scaling.
- **Multi-Period Segmentation**: Inspect progress aggregated across Today, This Week, and This Month.

### 🗓️ Interactive Calendar Matrix
- **Monthly Overview**: Month-by-month grid indicating completion statuses with visual dot indicators on each day.
- **Day Inspection**: Select any historical date to review past routines and completion records.
- **Quick Navigation**: Single-tap "Jump to Today" shortcut.

### 🏷️ Custom Categories
- **Visual Organization**: Organize tasks into color-coded categories with customizable Material icon badges.
- **Timeline Filters**: Filter your daily timeline on the fly by category chip.

### ⚙️ Preferences & Portability
- **Appearance Themes**: Fully dynamic Material 3 styling with System Default, Light Mode, and deep Midnight Dark Mode.
- **Time Formatting**: Native support for both 12-Hour (AM/PM) and 24-Hour clock formats.
- **First Day of Week**: Choose whether your calendar starts on Monday or Sunday.
- **Local Data Export & Import**: Export your complete database as a portable JSON file, and restore it at any time with zero lock-in.

---

## Screenshots

<div align="center">

| Today Screen | Calendar Matrix | Statistics & Heatmap | Categories | Settings |
| :---: | :---: | :---: | :---: | :---: |
| <img src="screenshots/today.png" width="180" alt="Today Screen" /> | <img src="screenshots/calendar.png" width="180" alt="Calendar Matrix" /> | <img src="screenshots/statistics.png" width="180" alt="Statistics & Heatmap" /> | <img src="screenshots/categories.png" width="180" alt="Categories Screen" /> | <img src="screenshots/settings.png" width="180" alt="Settings Screen" /> |

*(Screenshots can be contributed into the [`screenshots/`](screenshots/) directory)*

</div>

---

## Why Daynexa?

Most modern task trackers have transitioned into complex, cloud-dependent SaaS platforms that track user behavior, require recurring subscriptions, and fail when offline. 

**Daynexa was built on four foundational pillars:**
1. **Frictionless Simplicity**: Open the app and immediately see what matters today.
2. **True Ownership**: Your personal routine data belongs to you—stored exclusively on your physical device.
3. **Deterministic Reliability**: No unexpected cloud sync errors or network timeouts.
4. **Visual Motivation**: Meaningful analytics and heatmaps that celebrate daily consistency.

---

## Privacy

Daynexa is designed from the ground up as a **zero-telemetry, privacy-first** application:

- **Local Storage Only**: All routines, categories, and logs are saved in an on-device SQLite database via Room.
- **No Accounts Required**: No email addresses, phone numbers, or passwords needed.
- **Zero Cloud Databases**: No external sync servers, Firebase backends, or cloud storage.
- **Zero Telemetry & Analytics**: No Google Analytics, Mixpanel, Segment, or telemetry SDKs.
- **Zero Advertisements**: No ad banners, interstitials, or tracking SDKs.
- **Fully Offline**: All features run without an active internet connection.

---

## Offline Architecture

Daynexa follows modern Android development practices utilizing **Clean Architecture**, **MVVM (Model-View-ViewModel)**, and **Unidirectional Data Flow (UDF)**:

```
┌──────────────────────────────────────────────────────────┐
│                Jetpack Compose UI Screens                │
│         Today │ Calendar │ Statistics │ Settings         │
└────────────────────────────┬─────────────────────────────┘
                             │ StateFlow & UI Events
┌────────────────────────────▼─────────────────────────────┐
│                     ViewModel Layer                      │
│     TodayViewModel │ CalendarViewModel │ StatsViewModel  │
└────────────────────────────┬─────────────────────────────┘
                             │ Kotlin Coroutines & Flow
┌────────────────────────────▼─────────────────────────────┐
│                      Domain Layer                        │
│    TaskRepository │ SchedulingService │ Business Models  │
└────────────────────────────┬─────────────────────────────┘
                             │ Room DAOs
┌────────────────────────────▼─────────────────────────────┐
│                    Local SQLite (Room)                   │
│        CategoryDao │ TaskDao │ TaskOccurrenceDao         │
└──────────────────────────────────────────────────────────┘
```

For more details, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/) (2.2.x)
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3)
- **Architecture:** MVVM + Clean Architecture + Repository Pattern
- **Persistence:** [Android Room Database](https://developer.android.com/training/data-storage/room) (SQLite + KSP)
- **Asynchronous Flow:** Kotlin Coroutines & StateFlow
- **Navigation:** Jetpack Navigation Compose with type-safe routes
- **JSON Serialization:** [Moshi](https://github.com/square/moshi) with Kotlin Codegen
- **Testing:** JUnit 4, AndroidX Test, Robolectric

---

## Project Structure

```
daynexa/
├── .github/
│   ├── ISSUE_TEMPLATE/       # Bug report & feature request templates
│   ├── workflows/            # GitHub Actions CI/CD and Release pipelines
│   └── PULL_REQUEST_TEMPLATE.md
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/example/
│   │   │   │   ├── data/     # Room Database, DAOs, Entities, Repository impl
│   │   │   │   ├── domain/   # Domain Models, Repository interface, Services
│   │   │   │   ├── ui/       # Jetpack Compose Screens, Components, Theme
│   │   │   │   ├── DaynexaApplication.kt
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/          # Icons, Strings, Drawables
│   │   └── test/             # Unit tests and Robolectric test suite
│   └── build.gradle.kts
├── docs/                     # Architectural documentation
├── screenshots/              # Application preview captures
├── CHANGELOG.md              # Versioned release log
├── CONTRIBUTING.md           # Contributor workflow and guidelines
├── LICENSE                   # Open-source MIT License
├── README.md                 # Project documentation
└── settings.gradle.kts
```

---

## Building from Source

### Prerequisites
- Android Studio Ladybug (2024.2.1) or newer
- JDK 17
- Android SDK API 36 Platform Tools

### Build Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/niloymitra/daynexa.git
   cd daynexa
   ```

2. **Run Unit Tests:**
   ```bash
   ./gradlew :app:testDebugUnitTest
   ```

3. **Build Debug APK:**
   ```bash
   ./gradlew :app:assembleDebug
   ```
   *The generated APK will be available at:* `app/build/outputs/apk/debug/app-debug.apk`

4. **Build Release APK:**
   ```bash
   ./gradlew :app:assembleRelease
   ```
   *The generated APK will be available at:* `app/build/outputs/apk/release/app-release-unsigned.apk`

---

## CI/CD Pipeline

Daynexa includes an automated **GitHub Actions CI/CD pipeline** located at [`.github/workflows/android.yml`](.github/workflows/android.yml):

```
Developer Push / PR
        ↓
GitHub Actions (Ubuntu)
        ↓
Setup JDK 17 & Gradle Cache
        ↓
Run Unit Tests (:app:testDebugUnitTest)
        ↓
Build Release APK (:app:assembleRelease)
        ↓
Verify Output Integrity
        ↓
Upload Artifact (daynexa-release-apk)
```

### Downloading CI Builds
1. Navigate to the **Actions** tab in the GitHub repository.
2. Click on the latest workflow run on the `main` branch.
3. Scroll down to the **Artifacts** section.
4. Download the `daynexa-release-apk` archive to test the latest build.

---

## Download & Releases

Official tagged releases with pre-built signed/verified APKs can be found on the [GitHub Releases](https://github.com/niloymitra/daynexa/releases) page.

---

## Roadmap

### Current (MVP v0.1.0) — :white_check_mark: Completed
- [x] Weekly recurring & one-time task scheduling
- [x] Today timeline & real-time progress card
- [x] Monthly calendar matrix with productivity dots
- [x] Streak tracking & 14-week consistency heatmap
- [x] Custom categories with color palette & icon picker
- [x] Offline JSON backup export & restore
- [x] Material 3 Light/Dark/System themes & 12h/24h time formatting

### Planned Features (Future Releases) — 📌 Roadmap
- [ ] *Home Screen Widgets*: Quick glance at today's pending routines.
- [ ] *Local Notification Reminders*: Configurable on-device routine alert timers.
- [ ] *Encrypted Local Backups*: Optional passphrase encryption for JSON backup exports.
- [ ] *Extended Analytics*: Yearly activity breakdown and category distribution charts.
- [ ] *Wear OS Companion App*: Mark routines complete directly from your smartwatch.

*(Note: Diagnostic telemetry and usage tracking features are strictly not part of the MVP scope and remain planned for future exploration).*

---

## Contributing

Contributions are warmly welcomed! Please read our [CONTRIBUTING.md](CONTRIBUTING.md) guide and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) before submitting a Pull Request.

---

## License

Daynexa is open-source software licensed under the [MIT License](LICENSE).

---

## Developer & Showcase

Developed with ❤️ as an open-source Android showcase by **Niloy Mitra** and community contributors.

*Build Better Days with Daynexa.*
