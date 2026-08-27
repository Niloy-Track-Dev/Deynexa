# Changelog

All notable changes to **Daynexa** will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

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
