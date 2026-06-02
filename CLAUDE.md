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
