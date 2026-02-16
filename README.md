# Amigos

A macOS desktop app that helps the dispersed minded stay in touch with friends and loved ones.

You add your friends, how often you'd like to contact them, and through which channel (WhatsApp, iMessage, Email, SMS, Telegram). Amigos runs quietly in your menu bar and periodically nudges you to reach out — then opens the messaging app for you with one click.

## How It Works

### The Nudge System

Amigos uses a **staleness score** to decide who to nudge you about:

```
staleness = days_since_last_contact / desired_frequency_days
```

When a friend's staleness reaches **0.8** (80% of the desired interval), they become eligible for a nudge. Friends are selected using **weighted random selection** — the more overdue someone is, the more likely they'll be picked. The system also avoids nudging you about the same friend twice in a row.

### Scheduling

A background scheduler checks every **15 minutes** whether it's time to send a nudge. It respects your configured:

- **Nudges per week** (default: 3)
- **Active hours** (default: 9:00–21:00) — no nudges outside these hours
- **Notification days** (default: Mon, Wed, Fri) — only nudges on selected days

When a nudge fires, a macOS notification appears. Clicking it opens a small popup where you can:

- **Open [App]** — deep-links directly into the messaging app (e.g. `wa.me/` for WhatsApp)
- **Done** — logs the contact
- **Snooze** — dismisses for now (will come back later)
- **Skip** — marks as skipped in the log

### Data Storage

All data is stored locally in a SQLite database at `~/.amigos/amigos.db`. Nothing is sent to any server. The database contains:

- **Friends** — name, notes, desired contact frequency
- **Contact Channels** — per-friend channels with type and address (phone/email/username)
- **Contact Log** — history of prompts, contacts, and skips
- **App Settings** — key-value store for configuration

## Architecture

Single-process Kotlin/JVM app built with Compose Desktop and Material3. Four layers:

| Layer | Package | Responsibility |
|-------|---------|---------------|
| Data | `com.efetepe.amigos.data` | SQLDelight schema, repositories, channel types |
| Scheduler | `com.efetepe.amigos.scheduler` | Staleness scoring, background nudge loop |
| Notification | `com.efetepe.amigos.notification` | macOS notifications via osascript |
| UI | `com.efetepe.amigos.ui` | Compose Desktop windows and components |

The app lives in the **system tray** (menu bar). The main window has a NavigationRail sidebar with tabs for Friends, Log, Settings, and About. Closing the main window hides it to the tray — the scheduler keeps running in the background. Nudge popups appear as separate small windows.

## Tech Stack

- **Kotlin 2.1.21** on JVM with **Gradle 8.12** (Kotlin DSL)
- **Compose Desktop 1.7.3** — UI framework with Material3
- **SQLDelight 2.0.2** — typesafe SQLite via JDBC driver
- **kotlinx-coroutines 1.9.0** — background scheduler
- **kotlinx-datetime 0.6.1** — time handling

## Prerequisites

- **JDK 17+** (for building and running via Gradle)
- **macOS** (notifications use osascript, deep links use the `open` command)

## Development

### Run from source

```bash
./gradlew run
```

### Run tests

```bash
./gradlew test
```

### Run a specific test

```bash
./gradlew test --tests "com.efetepe.amigos.data.FriendRepositoryTest"
./gradlew test --tests "*.StalenessScorerTest.pickFriend*"
```

### Regenerate SQLDelight code

After editing `.sq` files in `src/main/sqldelight/`:

```bash
./gradlew generateMainAmigosDatabase
```

## Building the macOS App

```bash
./gradlew packageDmg
```

This produces `build/compose/binaries/main/dmg/Amigos-1.0.0.dmg`. Open the DMG, drag Amigos to `/Applications`, then right-click > Open the first time to bypass Gatekeeper (the app is not code-signed).

The packaged app bundles its own JVM runtime, so end users don't need Java installed.

### Auto-launch on Login

On first run from the packaged `.app`, Amigos installs a LaunchAgent plist at `~/Library/LaunchAgents/com.efetepe.amigos.plist` so it starts automatically when you log in. This only happens when running from a packaged `.app` (not during development via `./gradlew run`).

To disable auto-launch, delete the plist:

```bash
rm ~/Library/LaunchAgents/com.efetepe.amigos.plist
```

## Design Decisions

- **Local-only** — all data stays on your machine in SQLite, no accounts or cloud sync
- **Staleness-weighted random** — avoids strict round-robin; overdue friends get priority but there's still variety
- **Deep linking** — opens the actual messaging app with the contact pre-filled rather than trying to send messages itself
- **macOS notifications via osascript** — simplest approach for JVM apps on macOS, no native code needed
- **System tray app** — stays out of the way, runs in the background
- **LaunchAgent for auto-start** — standard macOS mechanism, user-scoped, easy to remove

## Credits

Icons by [Icons8](https://icons8.com/)
