# Architecture Documentation

## Architectural Overview

**Daynexa** is built following modern Android architectural recommendations (Clean Architecture + MVVM + Unidirectional Data Flow), tailored specifically for an **offline-first, zero-telemetry** environment.

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Layer (Jetpack Compose)               │
│   TodayScreen │ CalendarScreen │ StatsScreen │ Settings     │
└──────────────────────────────┬──────────────────────────────┘
                               │ StateFlow / Events
┌──────────────────────────────▼──────────────────────────────┐
│                    ViewModel Layer                          │
│   TodayViewModel │ CalendarViewModel │ StatisticsViewModel │
└──────────────────────────────┬──────────────────────────────┘
                               │ Kotlin Coroutines / Flow
┌──────────────────────────────▼──────────────────────────────┐
│                    Domain Layer                             │
│   Models │ TaskRepository (Interface) │ SchedulingService   │
└──────────────────────────────┬──────────────────────────────┘
                               │ Local DAO / Entities
┌──────────────────────────────▼──────────────────────────────┐
│                    Data Layer (Room DB)                     │
│   DaynexaDatabase │ TaskDao │ CategoryDao │ OccurrenceDao   │
└─────────────────────────────────────────────────────────────┘
```

---

## Key Principles

### 1. 100% Offline-First
All business logic, scheduling algorithms, and data persistence reside exclusively on the device:
- **Room Database**: SQLite wrapper offering compile-time query verification and Kotlin Coroutine support.
- **Moshi JSON Serialization**: For clean offline export and import of user backup bundles.
- **Zero Remote Dependencies**: The application does not instantiate network clients for user data operations.

### 2. Deterministic Daily Scheduling
Instead of generating physical database entries for every day far into the future:
- Recurring rules and time windows are evaluated dynamically via `SchedulingService`.
- Occurrences are logged only when states change (e.g., Completed, Skipped, Pending), keeping storage footprint minimal (< 5 MB).

### 3. Layer Separation
- **`ui/`**: Pure Jetpack Compose UI components, design tokens (`Theme.kt`, `Color.kt`, `Type.kt`), and navigation routes.
- **`domain/`**: Pure Kotlin models (`Task`, `Category`, `TaskOccurrence`), service logic (`SchedulingService`), and repository interfaces.
- **`data/`**: Room entities, DAOs, type converters, and repository implementations.

### 4. Privacy and Telemetry Invariance
The codebase deliberately omits tracking SDKs, cloud logins, advertising networks, and background diagnostic services.
