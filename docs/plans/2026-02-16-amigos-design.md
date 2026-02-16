# Amigos — Friend Contact Reminder

A local macOS app that nudges you to stay in touch with friends by sending notifications and making it easy to reach out via their preferred channel.

## Problem

Friends who live far away get forgotten — not because you don't care, but because out of sight is out of mind. Amigos fights that by proactively reminding you to reach out.

## Decisions

- **Platform:** macOS only (for now)
- **Architecture:** Compose Desktop monolith — single JVM process handles scheduling, notifications, and UI
- **Storage:** SQLite via SQLDelight (typesafe, single-file, no server)
- **Notifications:** macOS native via osascript (AppleScript bridge from JVM)
- **Interaction model:** Push-based. Background daemon sends notifications; clicking opens a small action window.
- **Scale:** 10-30 friends, 2-3 nudges per week

## Data Model

### Friends
| Column | Type | Notes |
|--------|------|-------|
| id | INTEGER PK | Auto-increment |
| name | TEXT | Friend's name |
| notes | TEXT? | Optional reminder notes |
| desired_frequency_days | INTEGER | Target contact interval (e.g., 30 = monthly) |

### Contact Channels
| Column | Type | Notes |
|--------|------|-------|
| id | INTEGER PK | Auto-increment |
| friend_id | INTEGER FK | References friends.id |
| channel_type | TEXT | WHATSAPP, IMESSAGE, EMAIL, SMS, TELEGRAM |
| address | TEXT | Phone number, email, or handle |
| preferred | INTEGER | Boolean — suggest this channel first |

### Contact Log
| Column | Type | Notes |
|--------|------|-------|
| id | INTEGER PK | Auto-increment |
| friend_id | INTEGER FK | References friends.id |
| channel_type | TEXT | Which channel was suggested |
| prompted_at | TEXT | ISO timestamp — when notification fired |
| contacted_at | TEXT? | ISO timestamp — when user confirmed contact |
| skipped | INTEGER | Boolean — if user dismissed/snoozed |

### App Settings
| Key | Default | Notes |
|-----|---------|-------|
| nudges_per_week | 3 | How many friends to suggest per week |
| active_hours_start | 09:00 | Don't notify before this time |
| active_hours_end | 21:00 | Don't notify after this time |
| notification_days | MON,WED,FRI | Which days to send nudges |

## Scheduling Algorithm

**Staleness score:** `days_since_last_contact / desired_frequency_days`
- Score >= 1.0 → overdue
- Score >= 0.8 → approaching due
- Score < 0.8 → recently contacted

**Selection:** When it's time to nudge:
1. Filter to friends with staleness >= 0.8
2. Weight selection probability by staleness score (more overdue = higher chance)
3. Add small random factor to avoid predictability
4. Never pick the same friend consecutively (unless they're the only one due)

**Timing:** Distribute nudges across configured days at random times within active-hours window.

## Notification & Action Flow

1. Scheduler picks a friend and fires a macOS notification via osascript
2. Notification shows: "Time to reach out to **[Name]** via [Channel]"
3. Clicking the notification opens a small Compose Desktop window (~400x300px):
   - Friend name + notes
   - "Open [App]" button with deep-link:
     - WhatsApp: `https://wa.me/<phone>`
     - iMessage: `imessage://<phone_or_email>`
     - Email: `mailto:<email>`
     - SMS: `sms:<phone>`
     - Telegram: `https://t.me/<handle>`
   - "Done" → logs contact as confirmed
   - "Snooze" → reschedules for tomorrow
   - "Skip" → logs as skipped
4. Menu bar tray icon keeps app alive. Right-click: Add Friend, View Log, Settings, Quit.

## Tech Stack

- **Kotlin/JVM** with Gradle (Kotlin DSL)
- **Compose Desktop** — UI framework (popup window + system tray)
- **SQLDelight** — typesafe SQLite (JDBC driver for desktop)
- **Kotlinx-coroutines** — background scheduler
- **Kotlinx-datetime** — date/time handling
- **osascript via ProcessBuilder** — macOS notifications

## Project Structure

```
amigos/
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
├── src/main/kotlin/com/efetepe/amigos/
│   ├── Main.kt                  # Entry point, tray icon
│   ├── data/
│   │   ├── Database.kt          # SQLDelight driver setup
│   │   └── models/              # Data classes
│   ├── scheduler/
│   │   ├── Scheduler.kt         # Core scheduling loop
│   │   └── StalenessScorer.kt   # Friend selection algorithm
│   ├── notification/
│   │   └── MacNotifier.kt       # osascript notification bridge
│   └── ui/
│       ├── ActionWindow.kt      # Popup Compose window
│       ├── AddFriendDialog.kt   # Add/edit friend
│       ├── LogView.kt           # Contact history view
│       └── SettingsView.kt      # App configuration
├── src/main/sqldelight/com/efetepe/amigos/
│   └── Amigos.sq                # SQL schema + queries
└── src/test/kotlin/             # Tests
```
