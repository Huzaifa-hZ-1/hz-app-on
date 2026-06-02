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

**UI layer:** XML layouts with ViewBinding enabled. `ActivityMainBinding` is inflated in `MainActivity` — all view references go through `binding`, never `findViewById`.

**Entry point:** `MainActivity` is the sole launcher activity. New screens should be added as additional `Activity` classes declared in `AndroidManifest.xml`, or as `Fragment`s hosted by `MainActivity`.

## Key Conventions

- ViewBinding is enabled — always use `binding.*` to access views, not `findViewById`.
- Dependencies use direct version strings (no version catalog). When adding a dependency, add it directly in `app/build.gradle.kts` under `dependencies {}`.
- Theme is `Theme.HzAppOn` (extends `MaterialComponents.DayNight.DarkActionBar`). Use Material Design components where possible.

## Adding New Screens

1. Create a new `Activity` in `app/src/main/java/com/hz/appon/`
2. Add a corresponding layout XML in `app/src/main/res/layout/`
3. Register the activity in `AndroidManifest.xml`

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
