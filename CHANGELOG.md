# Changelog

All notable changes to **Daynexa** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [0.6.0] - 2026-08-29

### Added
- **Full Data Portability & Backup Engine v2**: Complete offline backup and restore system with structured JSON serialization, format verification, and preview statistics.
- **Selective Restoration Modes**: Choose between *Merge (Recommended)* to preserve existing records and update entities, or *Replace / Overwrite* for complete clean restore.
- **Dedicated Focentra Focus Data Export**: Standalone JSON export specifically for imported Focentra study session history without mixing with local routine definitions.
- **Dynamic App Diagnostic Categories & Custom Categories**: Full custom category creation, editing, and deletion during app categorization and diagnostic flows with instant UI update.
- **Default Category Seeding**: Out-of-the-box preloaded categories (*Ai*, *System App*, *Productivity*, *Communication*, *Name Jap*, *Tools*, *Coding*, *Education*, *Study Timer*, *Social Media*, *Entertainment*, *Games*, *Browser*, *Utilities*).
- **100% Offline & Private Card**: Re-introduced prominent privacy badge in Settings detailing strictly local device persistence.

### Fixed
- Fixed category selection in App Usages Categories and App Diagnostic screens to allow adding custom categories inline.
- Resolved transaction handling and UI state synchronization during import/restore operations.

## [0.5.0] - 2026-08-28

### Added
- **Focentra App-to-App Integration**: Optional, privacy-first, 100% offline local Android IPC integration connecting Daynexa and Focentra with zero cloud servers or telemetry.
- **Secure IPC & Data Contract**: Structured local ContentProvider query protocol supporting schema versioning (`schemaVersion = 1`), duplicate prevention by `sessionId`, and explicit user consent flows.
- **Focus Time Priority & Anti-Double-Count Engine**: Prioritizes verified Focentra study sessions while preventing overlapping app usage duration double-counting in analytics.
- **Integration Settings & Management**: Dedicated Focentra section in Settings with connection status, one-tap sync, disconnect controls, and secure data clearing.
- **Room Database Migration (v4 -> v5)**: Added `focentra_study_sessions` table with optimized time-series indices.
- **Extended JSON Backup & Restore**: Upgraded backup engine to persist Focentra integration preferences and imported study session archives.

## [0.4.0] - 2026-08-28

### Added
- **Privacy-First Website & Browser Diagnostics Engine**: Implemented `WebsiteDiagnosticVpnService` using Android's local `VpnService` to capture outbound DNS lookups (UDP Port 53) strictly at the domain level.
- **Zero-Decryption Privacy Guarantee**: 100% on-device operation with zero HTTPS packet payload decryption, zero credential or cookie tracking, zero telemetry, and zero remote data transmission.
- **Domain Quality Ratings & Categories**: Built-in and user-customizable domain classifications across 6 quality tiers (*Very Good*, *Good*, *Neutral*, *Not Good*, *Bad*, *Very Bad*) and 11 categories (Education, Productivity, Social Media, Entertainment, News, etc.).
- **Custom Domain Rules Engine**: Rule-based categorization supporting *Exact Match*, *Subdomain*, and *Wildcard* matching patterns with active toggle switches and rule manager dialog.
- **Unified Diagnostics Dashboard**: Dual-section tab design integrating **Apps & Usage** and **Websites** diagnostics with synchronized date filters (Today, Weekly, Monthly, Custom Range).
- **Web Analytics & KPI Cards**: Real-time metrics for Total Visits, Estimated Browsing Time, Productive Domains, Distracting Domains, and Overall Web Productivity Ratio.
- **Interactive Daily Trend & Category Charts**: Visual bar charts and category distribution bars for daily web activities and app screen time.
- **Visited Domain Search & Filters**: Search domains by keyword with category filter chips and quick classification edit dialogs.
- **Room Database Migration (v3 -> v4)**: Added `website_classifications`, `website_events`, and `domain_rules` tables with optimized indices for fast time-series aggregation.
- **Extended JSON Backup & Portability**: Upgraded JSON backup and restore engine to serialize user domain rules and custom website quality ratings without data loss.

## [0.3.0] - 2026-08-28

### Added
- **Smart Task Reminders**: Offline, on-device routine alerts using Android `AlarmManager` and `NotificationManager` with high-priority channel support.
- **Customizable Reminder Offsets**: Choose reminder notifications *At start*, *5 minutes before*, *10 minutes before*, *15 minutes before*, or *30 minutes before* scheduled routines.
- **Device Reboot Resilience**: Implemented `BootCompletedReceiver` to restore and reschedule all routine alarms automatically upon device restart.
- **Advanced Productivity Score**: Dynamic 0–100 productivity score calculating consistency streaks, completion ratios, and routine regularity with performance ratings (*Elite*, *High*, *Moderate*, *Building*).
- **Weekly Comparison Deltas**: Real-time week-over-week performance delta indicator (`▲ +%` / `▼ -%`) on the analytics dashboard.
- **Notification Settings & Test Alert**: New "Notifications & Reminders" section in Settings to toggle notifications, configure default offsets, and trigger sample alert tests.
- **Room Database Migration (v2 -> v3)**: Added `reminderEnabled` and `reminderOffsetMinutes` schema support to the `tasks` entity with lossless migration.

## [0.2.0] - 2026-08-28

### Added
- **App Usage & Productivity Diagnostic Dashboard**: Real-time on-device screen time analytics, productivity rate calculations, and 7-day usage trend bar charts using Android `UsageStatsManager`.
- **App Quality Ratings**: Categorize applications as *Must Have*, *Nice to Have*, *Distraction*, or *Waste of Time*.
- **Multi-Category App Classifications**: Tag installed applications with multiple custom categories (e.g., Deep Work, Social Media, Entertainment, Communication) with search and edit dialogs.
- **Room Database Migration (v1 -> v2)**: Created `app_classifications` table for storing custom app ratings and category tags locally with zero telemetry.
- **Navigation Polish**: Hidden bottom navigation bar when viewing the App Usage Diagnostic and Classification screens for a clean full-screen experience.
- **UI Layout Optimization**: Refined Floating Action Button (FAB) placement across Today and Categories screens for optimal spacing above the bottom navigation bar.

## [0.1.0] - 2026-08-27

### Added
- **Initial Daynexa MVP Release**: Privacy-first, offline-first daily routine and habit management application.
- **Deterministic Routine Scheduling**: Support for weekly recurring schedules, day-of-week selectors (Weekdays, Weekends, Custom), all-day tasks, and specific time windows.
- **Today Dashboard**: Real-time progress metric card, active routine timeline, category filter chips, and smooth task state transitions (Pending, Completed, Skipped).
- **Interactive Calendar**: Monthly overview grid with daily productivity heat indicators, day-by-day task inspection, and one-tap return to today.
- **Analytics & Statistics**: KPI cards for Current Streak, Completion Rate, Peak Productive Day, and Total Routines Completed.
- **Consistency Heatmap**: 14-week visual activity heatmap with 4-level color intensity scaling.
- **Category Customization**: Color palette and icon picker with category-based task organization.
- **Local Data Persistence**: Offline SQLite database using Android Jetpack Room with full ACID compliance.
- **Backup & Portability**: Zero-friction JSON export and import for full offline data backup and restoration.
- **User Preferences**: Support for Light, Dark, and System appearance themes, 12h/24h time formatting, and customizable first day of the week (Monday/Sunday).
- **GitHub CI/CD**: Automated GitHub Actions workflow for test verification, APK assembly, and artifact generation.
