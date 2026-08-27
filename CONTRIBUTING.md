# Contributing to Daynexa

Thank you for your interest in contributing to **Daynexa**! We welcome bug fixes, documentation improvements, architectural refactoring, and enhancements that align with our core values.

---

## Core Project Philosophy

Before contributing, please keep in mind Daynexa's fundamental commitments:

1. **100% Offline-First**: All core tracking and scheduling must operate locally on-device without remote server dependencies.
2. **Zero Telemetry & Privacy-First**: No analytics, trackers, ads, or external data collection are permitted in the codebase.
3. **Deterministic & Lightweight**: Fast, responsive UI with Jetpack Compose and local Room persistence.
4. **Modularity & Clean Architecture**: Maintain separation between UI (`ui/`), Business Logic (`domain/`), and Data Storage (`data/`).

---

## Development Setup

### Prerequisites

- **Android Studio** Ladybug (2024.2.1+) or newer
- **JDK 17** (Temurin or OpenJDK recommended)
- **Android SDK API 36** platform tools installed

### Getting Started

1. **Fork the Repository** on GitHub.
2. **Clone your fork locally**:
   ```bash
   git clone https://github.com/<your-username>/daynexa.git
   cd daynexa
   ```
3. **Create a topic branch**:
   ```bash
   git checkout -b feature/my-enhancement
   # or
   git checkout -b fix/issue-description
   ```
4. **Open the project in Android Studio** and let Gradle sync.

---

## Running and Testing

### Run Unit Tests
To execute all local JVM unit tests:
```bash
./gradlew :app:testDebugUnitTest
```

### Build APK Locally
To verify the debug build:
```bash
./gradlew :app:assembleDebug
```

To build a release APK locally:
```bash
./gradlew :app:assembleRelease
```

---

## Submitting Changes

1. Ensure all unit tests pass cleanly.
2. Verify that no private files, keystores, or `.env` files are tracked.
3. Push your branch to your GitHub fork:
   ```bash
   git push origin feature/my-enhancement
   ```
4. Open a **Pull Request** against the `main` branch.
5. Fill out the PR template completely.
6. The automated CI workflow will run unit tests and verify the build.

---

## Code Style & Best Practices

- **Kotlin DSL & Jetpack Compose**: Follow standard Android Kotlin coding conventions and M3 guidelines.
- **State Flow**: Use `ViewModel` with `MutableStateFlow` and unidirectional data flow.
- **Resource Management**: String literals must reside in `res/values/strings.xml`.
- **Accessibility**: Include content descriptions on interactive and illustrative elements with minimum touch targets of 48dp.
