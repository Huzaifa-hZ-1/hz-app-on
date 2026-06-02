# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build Commands

```bash
# Build debug APK
./gradlew assembleDebug

# Build release APK
./gradlew assembleRelease

# Run unit tests
./gradlew test

# Run a single unit test class
./gradlew test --tests "com.hz.appon.ExampleUnitTest"

# Run instrumented tests (requires emulator or device)
./gradlew connectedAndroidTest

# Clean build
./gradlew clean

# Sync dependencies and check for issues
./gradlew build
```

On Windows use `gradlew.bat` instead of `./gradlew`.

## Project Configuration

- **Package:** `com.hz.appon`
- **Min SDK:** 26 (Android 8.0) — covers ~95% of active devices
- **Target/Compile SDK:** 35 (Android 15)
- **Language:** Kotlin, JVM target 11
- **Build system:** Gradle 8.9 with Kotlin DSL (`build.gradle.kts`)
- **AGP version:** 8.7.3

## Architecture

Single-module app (`:app`). No multi-module or flavour setup yet.

**Screaming architecture** — packages are organized by feature/domain, not by technical layer. The top-level package structure must communicate what the app *does*:
```
com.hz.appon/
├── <feature>/        e.g. quiz/, recipe/, puzzle/
│   ├── *Activity.kt
│   ├── *ViewModel.kt
│   ├── *Repository.kt  (only when there are 2+ data sources)
│   └── model/
├── ads/              AdMob wiring
└── shared/           Only for code genuinely used by 3+ features
```
Never create top-level `ui/`, `data/`, or `network/` packages.

**UI layer:** XML layouts with ViewBinding enabled. `ActivityMainBinding` is inflated in `MainActivity` — all view references go through `binding`, never `findViewById`.

**Entry point:** `MainActivity` is the sole launcher activity. New screens should be added as additional `Activity` classes declared in `AndroidManifest.xml`, or as `Fragment`s hosted by `MainActivity`.

## Testing Strategy

Every piece of logic that can be tested, must be tested. Test types in order of preference:

- **Unit tests** (`src/test/`) — pure logic, no Android framework. Use JUnit 4 + MockK.
- **Integration tests** (`src/test/` with Robolectric) — components working together without a device.
- **UI/Instrumentation tests** (`src/androidTest/`) — Espresso, only for critical user flows.

Test file mirrors source file: `quiz/QuizViewModel.kt` → `quiz/QuizViewModelTest.kt`.

Run a single test class: `./gradlew test --tests "com.hz.appon.quiz.QuizViewModelTest"`

## Over-Engineering Guardrails

Claude will explicitly flag with **[CAUTION: over-engineering]** before introducing any of the following unless the codebase already justifies it:
- Repository pattern with fewer than 2 real data sources
- Dependency injection (Hilt) with fewer than 3 injectable dependencies
- Sealed class/interface hierarchies for 2-state conditions
- Coroutine `Flow` where a single `suspend fun` suffices
- Abstract base classes or interfaces with only one implementation
- Multi-module splitting before the single module exceeds ~15 files per feature

## Coding Standards

### KDocs
Write KDocs on every `public` and `internal` class, interface, and function. Skip `private` and `override` — they're implementation detail.
Format:
```kotlin
/**
 * Brief one-line summary.
 *
 * Longer explanation only if the behaviour is non-obvious.
 *
 * @param categoryId OpenTDB category ID (9–32)
 * @return Empty list if no questions cached and offline
 */
```
Do NOT write KDocs that just restate the function name ("Returns the list of questions").

### Logging
Use **Timber** throughout — never `android.util.Log` directly.
- `Timber.d(...)` — debug info, flow tracing
- `Timber.w(...)` — unexpected but recoverable (e.g. empty API response)
- `Timber.e(throwable, ...)` — errors and exceptions
- Log at feature boundaries: repository calls, network responses, navigation events, gamification state changes
- Never log PII or user answers
- Timber is initialised in `App.onCreate()` with `DebugTree` in debug builds only — release builds produce no logs

### SOLID in practice
- **S** — one class, one reason to change. `LivesModule` only knows about hearts. `AdManager` only knows about ads.
- **O** — add `XpModule` without touching `LivesModule` or `GamificationEngine`
- **L** — `QuestionRepositoryImpl` substitutable for `QuestionRepository` everywhere
- **I** — `GamificationModule` has only the methods each module actually needs
- **D** — ViewModels depend on `QuestionRepository` interface, not `QuestionRepositoryImpl`

### General
- ViewBinding is enabled — always use `binding.*` to access views, not `findViewById`.
- Dependencies use direct version strings (no version catalog). When adding a dependency, add it directly in `app/build.gradle.kts` under `dependencies {}`.
- Theme is `Theme.HzAppOn` (extends `MaterialComponents.DayNight.DarkActionBar`). Use Material Design components where possible.
- Coroutines: `viewModelScope` in ViewModels, `Dispatchers.IO` for all DB/network work
- Expose `StateFlow` from ViewModels, never `MutableStateFlow`
- No hardcoded strings in Kotlin — all user-facing text in `strings.xml`

## Adding New Screens

1. Create a new `Activity` in `app/src/main/java/com/hz/appon/`
2. Add a corresponding layout XML in `app/src/main/res/layout/`
3. Register the activity in `AndroidManifest.xml`

## Developer Context

The developer is experienced with Kotlin but new to Android. When writing code, add short inline explanations for Android-specific concepts (lifecycle, Manifest, ViewBinding, Intents, ViewModel, etc.). Do NOT explain Kotlin language features — only Android-specific behaviour and the "why" behind it.

## Session Handoff

`doc/` is gitignored. It contains per-milestone MD files (`milestone-01-*.md`, `milestone-02-*.md`, …) and a `session-handoff.md` index. At the start of a new session, read `doc/session-handoff.md` to resume. At the end of a milestone, create a new `doc/milestone-NN-<topic>.md` and update the index.

## Windows Environment Notes

- Always prefix Gradle commands with `JAVA_HOME="/c/Program Files/Android/Android Studio/jbr"`
- `gh` CLI requires `export PATH="$PATH:/c/Program Files/GitHub CLI"` in Bash sessions
- SSH agent service is disabled on this machine — `git push` via SSH will fail. Workaround:
  `git remote set-url origin https://github.com/Huzaifa-hZ-1/hz-app-on.git && git push && git remote set-url origin git@github.com:Huzaifa-hZ-1/hz-app-on.git`

## Security Tooling

- Secret scanning: gitleaks runs as a pre-commit hook (`hooks/pre-commit`, config in `.gitleaks.toml`)
- Hooks are tracked in `hooks/` — git is configured with `core.hooksPath = hooks`
- CI runs gitleaks, Snyk, and SonarCloud on every push to `main`
- Snyk CI requires `SNYK_TOKEN` secret — add at github.com/Huzaifa-hZ-1/hz-app-on/settings/secrets
- SonarCloud CI requires `SONAR_TOKEN` secret — sign up at sonarcloud.io first

## Release & AdMob

- Keystore at `app/keystore/hz-app-on.jks` (gitignored). Credentials in `local.properties` (gitignored).
- `local.properties` keystore path is relative to the `:app` module dir, not the project root
- AdMob test App ID is in `AndroidManifest.xml` — replace with real ID from admob.google.com before publishing
