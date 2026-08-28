# Architecture Documentation

## Architectural Overview

**Daynexa** is built following modern Android architectural recommendations (Clean Architecture + MVVM + Unidirectional Data Flow), tailored specifically for an **offline-first, zero-telemetry** environment.

```
┌───────────────────────────────────────────────────────────────────┐
│                    UI Layer (Jetpack Compose)                     │
│ TodayScreen │ CalendarScreen │ StatsScreen │ Settings │ Diagnostic│
│           (App Diagnostics Tab & Website Diagnostics Tab)         │
└──────────────────────────────┬────────────────────────────────────┘
                               │ StateFlow / Events
┌──────────────────────────────▼────────────────────────────────────┐
│                    ViewModel Layer                                │
│ TodayViewModel │ CalendarViewModel │ StatsViewModel │ Diagnostic  │
└──────────────────────────────┬────────────────────────────────────┘
                               │ Kotlin Coroutines / Flow
┌──────────────────────────────▼────────────────────────────────────┐
│                    Domain Layer                                   │
│ TaskRepository │ DiagnosticRepo │ WebsiteDiagnosticRepo │ Services │
│          (SchedulingService, WebsiteDiagnosticVpnService)         │
└──────────────────────────────┬────────────────────────────────────┘
                               │ Local DAOs / Entities & Packet DNS
┌──────────────────────────────▼────────────────────────────────────┐
│                    Data Layer (Room DB v4)                        │
│ TaskDao │ CategoryDao │ OccurrenceDao │ AppClassDao │ WebClassDao │
│              WebsiteEventDao │ DomainRuleDao                      │
└───────────────────────────────────────────────────────────────────┘
```

---

## Subsystem Architecture

### 1. 100% Offline-First Data Architecture
All business logic, scheduling algorithms, and data persistence reside exclusively on the device:
- **Room Database**: SQLite wrapper offering compile-time query verification and Kotlin Coroutine support.
- **Moshi JSON Serialization**: For clean offline export and import of user backup bundles.
- **Zero Remote Dependencies**: The application does not instantiate network clients or external tracking servers for user data operations.

### 2. Deterministic Daily Scheduling Engine
Instead of generating physical database entries for every day far into the future:
- Recurring rules and time windows are evaluated dynamically via `SchedulingService`.
- Occurrences are logged only when states change (e.g., Completed, Skipped, Pending), keeping storage footprint minimal (< 5 MB).

### 3. Smart Task Reminders & Reboot Persistence
- **`TaskAlarmScheduler`**: Leverages Android's `AlarmManager` (`setExactAndAllowWhileIdle` / `setWindow`) with support for exact start alarms and customizable pre-alerts (5m, 10m, 15m, 30m).
- **`BootCompletedReceiver`**: Automatically triggers on `ACTION_BOOT_COMPLETED` and `ACTION_MY_PACKAGE_REPLACED` to query active tasks and restore alarms across device restarts.
- **`TaskReminderReceiver`**: Emits high-priority notification channels with direct action intents back into Daynexa.

### 4. App Usage & Productivity Diagnostics
- **`UsageStatsManager` Engine**: Queries system-level foreground statistics (`PACKAGE_USAGE_STATS`) across configurable intervals (Today, Weekly, Monthly, Custom Range).
- **App Quality Ratings**: Categorizes applications into *Must Have*, *Nice to Have*, *Distraction*, or *Waste of Time*.
- **Category Tagging**: Maps installed apps to multiple custom categories for screen time breakdown and productivity ratio computation.

### 5. Website & Browser Diagnostics Engine
- **`WebsiteDiagnosticVpnService`**: Establishes an Android local VPN tunnel capturing outbound DNS queries (UDP Port 53).
- **DNS Packet Parser**: Safely extracts the domain name (`QNAME`) from the raw byte buffer with **zero HTTPS payload inspection**, zero cookie access, and zero credential reading.
- **Domain Matching Rules Engine**: Evaluates visited domains against custom user rules supporting *Exact Match*, *Subdomain*, and *Wildcard* patterns.
- **Local Time-Series Storage**: Records timestamped domain events into Room `website_events` with indexed queries for fast aggregation.

### 6. Room Database Evolution (Migrations v1 -> v4)
- **Version 1**: Initial release schema (`categories`, `tasks`, `task_occurrences`).
- **Version 2**: Added `app_classifications` table for app ratings and category tags.
- **Version 3**: Added `reminderEnabled` and `reminderOffsetMinutes` columns to `tasks`.
- **Version 4**: Added `website_classifications`, `website_events`, and `domain_rules` tables with indices on `timestamp` and `domain`.

---

## Layer Separation Principles

- **`ui/`**: Pure Jetpack Compose UI components, design tokens (`Theme.kt`, `Color.kt`, `Type.kt`), and navigation routes.
- **`domain/`**: Pure Kotlin models (`Task`, `Category`, `TaskOccurrence`, `WebsiteEvent`, `DomainRule`), service logic (`SchedulingService`, `WebsiteDiagnosticVpnService`), and repository interfaces.
- **`data/`**: Room entities, DAOs, type converters, and repository implementations.
- **`receiver/`**: Android system broadcast receivers for alarms and reboot restoration.

---

## Privacy and Telemetry Invariance
The codebase deliberately omits tracking SDKs, cloud logins, advertising networks, and remote telemetry services. All diagnostics and usage data remain strictly on the user's physical hardware.

