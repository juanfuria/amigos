# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Amigos is a local macOS desktop app that reminds you to stay in touch with friends. It runs as a system tray icon, sends macOS notifications when a friend is "due" for contact, and shows a small popup window to open the messaging app and log the interaction.

## Build & Run Commands

```bash
./gradlew build          # Build the project
./gradlew run            # Run the app (appears in system tray)
./gradlew test           # Run all tests
./gradlew test --tests "com.efetepe.amigos.data.FriendRepositoryTest"  # Single test class
./gradlew test --tests "*.StalenessScorerTest.pickFriend*"             # Test name pattern
./gradlew generateMainAmigosDatabase  # Regenerate SQLDelight code after .sq changes
./gradlew packageDmg     # Package as macOS .dmg
```

## Tech Stack

- **Kotlin 2.1.21** / JVM with **Gradle 8.12** (Kotlin DSL)
- **Compose Desktop 1.7.3** (org.jetbrains.compose) — UI framework with Material3
- **SQLDelight 2.0.2** — typesafe SQLite via JDBC driver, generates Kotlin from `.sq` files
- **kotlinx-coroutines 1.9.0** — background scheduler
- **kotlinx-datetime 0.6.1** — uses `kotlinx.datetime.Clock.System`, `Instant`, `LocalTime`

## Architecture

Single-process Compose Desktop app with four layers:

**Data layer** (`com.efetepe.amigos.data`):
- SQLDelight `.sq` files in `src/main/sqldelight/` define schema and queries
- `DatabaseFactory` creates on-disk SQLite at `~/.amigos/amigos.db` (or in-memory for tests)
- `FriendRepository` wraps all CRUD for friends, contact channels, and contact log
- `SettingsRepository` wraps key-value app settings with typed Kotlin properties
- `ChannelType` enum defines supported channels (WHATSAPP, IMESSAGE, EMAIL, SMS, TELEGRAM) with deep-link URI prefixes

**Scheduler** (`com.efetepe.amigos.scheduler`):
- `StalenessScorer` computes `days_since_last_contact / desired_frequency_days` and picks friends using weighted random selection (threshold >= 0.8)
- `Scheduler` runs a coroutine loop every 15 minutes, checks if current time is within configured notification schedule, picks a friend, fires macOS notification, and invokes callback

**Notification** (`com.efetepe.amigos.notification`):
- `MacNotifier` sends native macOS notifications via `osascript` (AppleScript) through `ProcessBuilder`

**UI** (`com.efetepe.amigos.ui`):
- `ActionWindow` — popup when nudge fires: friend name, "Open [App]" deep-link, Done/Snooze/Skip
- `AddFriendDialog` — form with name, notes, frequency, and dynamic channel list
- `LogView` — scrollable contact history
- `SettingsView` — nudges/week, active hours, notification days (FilterChip selection)
- `DeepLinker` — opens messaging apps via `java.awt.Desktop.browse()` with URI schemes

**Main** (`com.efetepe.amigos.Main`):
- `AppScreen` enum drives window routing (NONE = tray only, ACTION, ADD_FRIEND, LOG, SETTINGS)
- System tray icon with right-click menu is the app's home state

## Key Patterns

- SQLDelight generates code into `com.efetepe.amigos.data` — after editing `.sq` files, run `generateMainAmigosDatabase`
- Tests use `DatabaseFactory.createInMemory()` for isolated SQLite instances
- The `Tray` composable expects a `BitmapPainter` (not raw BufferedImage) — see `createTrayIcon()` in Main.kt
- Timestamps stored as ISO-8601 strings in SQLite, parsed with `Instant.parse()`
- Settings stored as key-value pairs in `app_settings` table, serialized/deserialized in `SettingsRepository`
