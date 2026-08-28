# Daynexa

<div align="center">

<h3>Build Better Days.</h3>

<p>A privacy-first, 100% offline Android daily routine and habit management application built with Jetpack Compose, Kotlin Coroutines, and Room.</p>

<p>
  <a href="https://github.com/Niloy-Track-Dev/Deynexa/releases">
    <img src="https://img.shields.io/badge/Download%20Apk-Latest%20Release-2ea44f?style=for-the-badge&logo=android&logoColor=white" alt="Download Apk" />
  </a>
  <a href="https://github.com/Niloy-Track-Dev/Deynexa/actions/workflows/android.yml">
    <img src="https://img.shields.io/badge/CI%2FCD-Automated-0969da?style=for-the-badge&logo=githubactions&logoColor=white" alt="CI Status" />
  </a>
</p>

<p>
  <a href="https://github.com/Niloy-Track-Dev/Deynexa/releases"><img src="https://img.shields.io/badge/Release-v0.4.0-blue.svg" alt="Latest Release" /></a>
  <a href="LICENSE"><img src="https://img.shields.io/badge/License-MIT-emerald.svg" alt="License: MIT" /></a>
  <a href="https://developer.android.com"><img src="https://img.shields.io/badge/Platform-Android%208.0%2B%20%28API%2026%2B%29-brightgreen.svg" alt="Platform" /></a>
  <a href="#offline-architecture"><img src="https://img.shields.io/badge/Architecture-100%25%20Offline--First-blueviolet.svg" alt="Offline-First" /></a>
  <a href="#privacy-guarantees"><img src="https://img.shields.io/badge/Telemetry-Zero%20Tracking-black.svg" alt="Zero Telemetry" /></a>
</p>

<p>
  👉 <a href="https://github.com/Niloy-Track-Dev/Deynexa/releases"><strong>[ Download Apk (GitHub Releases) ]</strong></a> 👈 • <a href="https://github.com/Niloy-Track-Dev/Deynexa/issues/new?template=bug_report.md"><strong>Report Bug</strong></a> • <a href="https://github.com/Niloy-Track-Dev/Deynexa/issues/new?template=feature_request.md"><strong>Request Feature</strong></a> • <a href="docs/ARCHITECTURE.md"><strong>Documentation</strong></a>
</p>

</div>

---

## 📑 Table of Contents

- [Overview](#overview)
- [📥 Download Apk & Releases](#download-apk)
- [Key Features](#features)
  - [Routine & Habit Scheduling](#-routine--habit-scheduling)
  - [Visual Analytics & Consistency Heatmap](#-visual-analytics--consistency-heatmap)
  - [App Usage & Productivity Diagnostics](#-app-usage--productivity-diagnostics)
  - [Browser & Website Diagnostics](#-browser--website-diagnostics)
  - [Smart Routine Reminders](#-smart-routine-reminders)
  - [Interactive Calendar Matrix](#-interactive-calendar-matrix)
  - [Custom Categories & Tagging](#-custom-categories--tagging)
  - [Preferences & Portability](#-preferences--portability)
- [Screenshots & Visual Preview](#screenshots)
- [Why Daynexa?](#why-daynexa)
- [Privacy Guarantees](#privacy-guarantees)
- [Offline Architecture](#offline-architecture)
- [Tech Stack](#tech-stack)
- [Project Structure](#project-structure)
- [Building from Source](#building-from-source)
- [CI/CD Automation](#cicd-pipeline)
- [Roadmap](#roadmap)
- [Contributing](#contributing)
- [Security](#security)
- [License](#license)

---

## Overview

**Daynexa** is a lightweight, distraction-free productivity application engineered to help you design, track, and sustain meaningful daily routines. Unlike conventional productivity tools that require cloud accounts, display advertisements, or monetize your activity logs, Daynexa operates **100% locally on your device**.

Featuring deterministic schedule computation, an interactive monthly calendar matrix, visual streak analytics, a 14-week consistency heatmap, **on-device app & website diagnostics**, **smart routine alarms**, and complete JSON backup export/import, Daynexa gives you total control over your habits and digital wellbeing without sacrificing your privacy.

---

## <a id="download-apk"></a>📥 Download Apk

You can download the latest signed production APK directly from the GitHub Releases section:

👉 **[Go to GitHub Releases Section & Download Apk](https://github.com/Niloy-Track-Dev/Deynexa/releases)** 👈

### Installation Steps
1. Navigate to the **[Releases](https://github.com/Niloy-Track-Dev/Deynexa/releases)** section.
2. Under the latest release (e.g. `v0.4.0`), look under **Assets**.
3. Download the file **`Daynexa-v0.4.0-release.apk`**.
4. Open the APK file on your Android device (Android 8.0+ / API 26+) and confirm installation.

---

## Features

### 📅 Routine & Habit Scheduling
- **Deterministic Scheduling**: Configure one-off tasks or recurring routines on specific days of the week (Every Day, Weekdays, Weekends, or custom selections).
- **Flexible Time Windows**: Support for scheduled time ranges with Material 3 time pickers, or all-day flexible habits.
- **Task State Progression**: Effortlessly toggle between **Pending**, **Completed**, and **Skipped** states with responsive feedback and smooth animations.

### 📊 Visual Analytics & Consistency Heatmap
- **Productivity Dashboard**: Monitor key performance metrics including Current Daily Streak, Completion Rate, Peak Productive Day, and Total Completed Routines.
- **14-Week Consistency Heatmap**: GitHub-style activity grid visualizing daily habit execution over time with 4-level color intensity scaling.
- **Multi-Period Segmentation**: Inspect progress aggregated across Today, This Week, and This Month with dynamic week-over-week productivity score deltas.

### 📱 App Usage & Productivity Diagnostics
- **On-Device Screen Time Analysis**: Tracks foreground app duration, launch frequencies, and daily trend bar charts using Android `UsageStatsManager`.
- **Quality Ratings**: Classify applications into *Must Have*, *Nice to Have*, *Distraction*, or *Waste of Time*.
- **Custom Category Tagging**: Assign multiple custom category tags to apps (Deep Work, Social, Dev, Entertainment) to visualize productive vs non-productive screen time.

### 🌐 Browser & Website Diagnostics
- **Privacy-First Domain Capture**: On-device DNS packet interception (Port 53) via local `VpnService` with **zero HTTPS payload inspection**, zero cookies, and zero cloud transmission.
- **Website Quality & Classification**: Categorize domains (*Very Good*, *Good*, *Neutral*, *Not Good*, *Bad*, *Very Bad*) across Education, Productivity, Social Media, Entertainment, and News.
- **Custom Domain Rules Engine**: Define custom matching rules (Exact, Subdomain, Wildcard) with instant toggle and deletion controls.
- **Rich Web Analytics**: Domain search, category filter chips, visit counters, estimated browsing durations, and daily web activity trends.

### ⏰ Smart Routine Reminders
- **Exact & Inexact Alarms**: System-level notifications scheduled via Android `AlarmManager` with high-importance notification channels.
- **Customizable Offsets**: Set alerts *At start*, *5m*, *10m*, *15m*, or *30m* prior to routine start time.
- **Reboot Auto-Restoration**: `BootCompletedReceiver` automatically reschedules pending alarms on device restart.

### 🗓️ Interactive Calendar Matrix
- **Monthly Overview**: Month-by-month grid indicating completion statuses with visual dot indicators on each day.
- **Day Inspection**: Select any historical date to review past routines and completion records.
- **Quick Navigation**: Single-tap "Jump to Today" shortcut.

### 🏷️ Custom Categories & Tagging
- **Visual Organization**: Organize tasks into color-coded categories with customizable Material icon badges.
- **Timeline Filters**: Filter your daily timeline on the fly by category chip.

### ⚙️ Preferences & Portability
- **Appearance Themes**: Fully dynamic Material 3 styling with System Default, Light Mode, and deep Midnight Dark Mode.
- **Time Formatting**: Native support for both 12-Hour (AM/PM) and 24-Hour clock formats.
- **First Day of Week**: Choose whether your calendar starts on Monday or Sunday.
- **Local Data Export & Import**: Export your complete database as a portable JSON file (including routines, app classifications, website classifications, and custom rules), and restore it at any time.

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

## Privacy Guarantees

Daynexa is designed from the ground up as a **zero-telemetry, privacy-first** application:

- 🔒 **Local Storage Only**: All routines, categories, and logs are saved in an on-device SQLite database via Room.
- 👤 **No Accounts Required**: No email addresses, phone numbers, or passwords needed.
- ☁️ **Zero Cloud Databases**: No external sync servers, Firebase backends, or cloud storage.
- 🚫 **Zero Telemetry & Analytics**: No Google Analytics, Mixpanel, Segment, or telemetry SDKs.
- 🛡️ **Zero Advertisements**: No ad banners, interstitials, or tracking SDKs.
- 🔌 **Fully Offline**: All features run without an active internet connection.

---

## Offline Architecture

Daynexa follows modern Android development practices utilizing **Clean Architecture**, **MVVM (Model-View-ViewModel)**, and **Unidirectional Data Flow (UDF)**:

```
┌──────────────────────────────────────────────────────────┐
│                Jetpack Compose UI Screens                │
│   Today │ Calendar │ Statistics │ Settings │ Diagnostic  │
└────────────────────────────┬─────────────────────────────┘
                             │ StateFlow & UI Events
┌────────────────────────────▼─────────────────────────────┐
│                     ViewModel Layer                      │
│ TodayViewModel │ CalendarViewModel │ StatsViewModel │... │
└────────────────────────────┬─────────────────────────────┘
                             │ Kotlin Coroutines & Flow
┌────────────────────────────▼─────────────────────────────┐
│                      Domain Layer                        │
│ TaskRepository │ DiagnosticRepo │ WebsiteRepo │ Services │
└────────────────────────────┬─────────────────────────────┘
                             │ Local DAOs & Packet Parsing
┌────────────────────────────▼─────────────────────────────┐
│              Local SQLite (Room) & VpnService            │
│  CategoryDao │ TaskDao │ AppClassDao │ WebClassDao │...  │
└──────────────────────────────────────────────────────────┘
```

For more architectural details, see [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md).

---

## Tech Stack

| Domain | Technology / Library | Purpose |
| :--- | :--- | :--- |
| **Language** | [Kotlin](https://kotlinlang.org/) (2.2.x) | Modern, expressive Android development |
| **UI Framework** | [Jetpack Compose](https://developer.android.com/jetpack/compose) | Declarative Material 3 user interface |
| **Architecture** | MVVM + Clean Architecture | Unidirectional Data Flow and separation of concerns |
| **Persistence** | [Android Room](https://developer.android.com/training/data-storage/room) | Local SQLite persistence via Kotlin Symbol Processing (KSP) |
| **Diagnostics** | `UsageStatsManager` & `VpnService` | On-device app usage & zero-decryption DNS domain capture |
| **Scheduling** | `AlarmManager` & `BroadcastReceiver` | Exact & inexact notifications with reboot persistence |
| **Concurrency** | Kotlin Coroutines & StateFlow | Reactive and asynchronous state streaming |
| **Navigation** | Navigation Compose | Type-safe declarative screen routing |
| **Serialization** | [Moshi](https://github.com/square/moshi) | Fast, safe JSON serialization for local backup & restore |
| **Testing** | JUnit 4 & Robolectric | Unit and JVM integration tests |

---

## Project Structure

```
daynexa/
├── .github/
│   ├── ISSUE_TEMPLATE/       # Structured bug report & feature request templates
│   ├── workflows/            # GitHub Actions CI/CD (android.yml)
│   └── PULL_REQUEST_TEMPLATE.md
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com.niloy/
│   │   │   │   ├── data/     # Room Database, DAOs, Entities, Repository impl
│   │   │   │   ├── domain/   # Domain Models, Repository interface, Services (VPN)
│   │   │   │   ├── ui/       # Jetpack Compose Screens, Components, Theme
│   │   │   │   ├── DaynexaApplication.kt
│   │   │   │   └── MainActivity.kt
│   │   │   └── res/          # Icons, Strings, Drawables
│   │   └── test/             # Unit tests and Robolectric test suite
│   └── build.gradle.kts
├── docs/                     # Architectural documentation
├── screenshots/              # Application preview captures
├── CHANGELOG.md              # Versioned release log
├── CODE_OF_CONDUCT.md        # Contributor code of conduct
├── CONTRIBUTING.md           # Contributor workflow and guidelines
├── LICENSE                   # Open-source MIT License
├── README.md                 # Project documentation
├── SECURITY.md               # Security & privacy policy
└── settings.gradle.kts
```

---

## Building from Source

### Prerequisites
- **Android Studio** Ladybug (2024.2.1) or newer
- **JDK 17** (Temurin or OpenJDK recommended)
- **Android SDK API 36** Platform Tools

### Build Instructions

1. **Clone the repository:**
   ```bash
   git clone https://github.com/Niloy-Track-Dev/Deynexa.git
   cd Deynexa
   ```

2. **Run Unit Tests:**
   ```bash
   gradle :app:testDebugUnitTest
   ```

3. **Build Debug APK:**
   ```bash
   gradle :app:assembleDebug
   ```
   *The generated APK will be available at:* `app/build/outputs/apk/debug/app-debug.apk`

4. **Build Release APK:**
   ```bash
   gradle :app:assembleRelease
   ```
   *The generated APK will be available at:* `app/build/outputs/apk/release/app-release.apk`

---

## CI/CD Pipeline

Daynexa includes an automated **GitHub Actions CI/CD pipeline** located at [`.github/workflows/android.yml`](.github/workflows/android.yml):

```
Developer Push / Pull Request
              ↓
GitHub Actions (Ubuntu Latest)
              ↓
Setup JDK 17 & Android SDK Tools
              ↓
Run Unit Tests (:app:testDebugUnitTest)
              ↓
Build Signed Release APK (:app:assembleRelease)
              ↓
Publish GitHub Release & Upload APK Artifact
```

---

## Roadmap

### Current (Release v0.4.0) — ✅ Completed
- [x] Weekly recurring & one-time task scheduling
- [x] Today timeline & real-time progress card
- [x] Monthly calendar matrix with productivity dots
- [x] Streak tracking & 14-week consistency heatmap
- [x] Custom categories with color palette & icon picker
- [x] Offline JSON backup export & restore
- [x] Material 3 Light/Dark/System themes & 12h/24h time formatting
- [x] Direct in-app release download actions and GitHub repository shortcuts
- [x] **App Usage & Productivity Diagnostic System**: On-device usage stats monitoring (`PACKAGE_USAGE_STATS`)
- [x] **App Quality Ratings**: *Must Have*, *Nice to Have*, *Distraction*, *Waste of Time*
- [x] **Multi-Category App Classifications**: Categorize installed apps with search & edit capabilities
- [x] **Browser & Website Diagnostics**: Zero-decryption DNS packet capture on-device via `VpnService`
- [x] **Website Quality & Classification**: Categorize visited web domains with custom ratings & categories
- [x] **Custom Domain Matching Rules**: Exact, subdomain, and wildcard pattern evaluation
- [x] **Smart Task Reminders**: Exact and inexact `AlarmManager` routine alerts with customizable offsets (At start, 5m, 10m, 15m, 30m)
- [x] **Device Reboot Alarm Restoration**: `BootCompletedReceiver` for automated persistent alarm scheduling
- [x] **Advanced Productivity Score**: Dynamic 0-100 metric with consistency and streak ratings
- [x] **Week-over-Week Productivity Deltas**: Visual performance trend indicators
- [x] **Room Migration v1 -> v2 -> v3 -> v4**: Lossless database evolution across all entities & indices

### Planned Features (Future Releases) — 📌 Roadmap
- [ ] *Home Screen Widgets*: Quick glance at today's pending routines.
- [ ] *Encrypted Local Backups*: Optional passphrase encryption for JSON backup exports.
- [ ] *Extended Analytics*: Yearly activity breakdown and category distribution charts.
- [ ] *Wear OS Companion App*: Mark routines complete directly from your smartwatch.

---

## Contributing

Contributions are warmly welcomed! Please read our [CONTRIBUTING.md](CONTRIBUTING.md) guide and [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) before submitting a Pull Request.

---

## Security

Please review our [SECURITY.md](SECURITY.md) policy for instructions on reporting vulnerabilities and privacy concerns.

---

## License

Daynexa is open-source software licensed under the [MIT License](LICENSE).

---

<div align="center">

Developed with ❤️ as an open-source Android showcase by **Niloy Mitra** and community contributors.

*Build Better Days with Daynexa.*

</div>
