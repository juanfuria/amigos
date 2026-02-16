# Amigos Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Build a macOS desktop app that reminds you to contact friends via notifications, with a small Compose Desktop action window for confirming/skipping contact.

**Architecture:** Single Compose Desktop (JVM) process. Background coroutine scheduler picks friends using staleness-weighted random selection and fires macOS notifications via osascript. A system tray icon keeps the app alive. SQLDelight provides typesafe SQLite access.

**Tech Stack:** Kotlin 2.3.10, Compose Multiplatform 1.10.1, SQLDelight 2.2.1, kotlinx-datetime 0.7.1, kotlinx-coroutines, Gradle Kotlin DSL.

---

## Task 1: Project Scaffolding

Set up the Gradle build, directory structure, and verify the project compiles and runs a blank window.

**Files:**
- Create: `settings.gradle.kts`
- Create: `build.gradle.kts`
- Create: `gradle.properties`
- Create: `src/main/kotlin/com/efetepe/amigos/Main.kt`

**Step 1: Initialize Gradle wrapper**

Run:
```bash
cd /Users/juan/repos/efetepe/amigos
gradle wrapper --gradle-version 8.12
```
Expected: `gradlew`, `gradlew.bat`, `gradle/wrapper/` created.

**Step 2: Create `settings.gradle.kts`**

```kotlin
rootProject.name = "amigos"

pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
        mavenCentral()
    }
}
```

**Step 3: Create `gradle.properties`**

```properties
org.gradle.jvmargs=-Xmx2048m
kotlin.code.style=official
```

**Step 4: Create `build.gradle.kts`**

```kotlin
import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    kotlin("jvm") version "2.1.21"
    id("org.jetbrains.compose") version "1.7.3"
    id("org.jetbrains.kotlin.plugin.compose") version "2.1.21"
    id("app.cash.sqldelight") version "2.0.2"
}

group = "com.efetepe.amigos"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    google()
}

dependencies {
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)

    // SQLDelight
    implementation("app.cash.sqldelight:sqlite-driver:2.0.2")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.9.0")

    // DateTime
    implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.6.1")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
}

compose.desktop {
    application {
        mainClass = "com.efetepe.amigos.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "Amigos"
            packageVersion = "1.0.0"
            macOS {
                bundleID = "com.efetepe.amigos"
            }
        }
    }
}

sqldelight {
    databases {
        create("AmigosDatabase") {
            packageName.set("com.efetepe.amigos.data")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}
```

Note: Use Kotlin 2.1.21 + Compose 1.7.3 (known compatible pair). SQLDelight 2.0.2 and kotlinx-datetime 0.6.1 for stable compatibility. If version conflicts arise at build time, adjust to the latest compatible set.

**Step 5: Create minimal `Main.kt`**

File: `src/main/kotlin/com/efetepe/amigos/Main.kt`
```kotlin
package com.efetepe.amigos

import androidx.compose.material3.Text
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Amigos") {
        Text("Amigos is running!")
    }
}
```

**Step 6: Verify build compiles**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 7: Verify app launches**

Run: `./gradlew run`
Expected: A window appears with "Amigos is running!" text. Close it manually.

**Step 8: Commit**

```bash
git add settings.gradle.kts build.gradle.kts gradle.properties gradlew gradlew.bat gradle/ src/
git commit -m "feat: scaffold Compose Desktop project with Gradle build"
```

---

## Task 2: SQLDelight Schema & Database Setup

Define the SQL schema and set up the database driver. Verify SQLDelight generates code.

**Files:**
- Create: `src/main/sqldelight/com/efetepe/amigos/data/Friend.sq`
- Create: `src/main/sqldelight/com/efetepe/amigos/data/ContactChannel.sq`
- Create: `src/main/sqldelight/com/efetepe/amigos/data/ContactLog.sq`
- Create: `src/main/sqldelight/com/efetepe/amigos/data/AppSettings.sq`
- Create: `src/main/kotlin/com/efetepe/amigos/data/DatabaseFactory.kt`

**Step 1: Create `Friend.sq`**

File: `src/main/sqldelight/com/efetepe/amigos/data/Friend.sq`
```sql
CREATE TABLE friend (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    notes TEXT,
    desired_frequency_days INTEGER NOT NULL DEFAULT 30
);

selectAll:
SELECT * FROM friend ORDER BY name;

selectById:
SELECT * FROM friend WHERE id = ?;

insert:
INSERT INTO friend (name, notes, desired_frequency_days)
VALUES (?, ?, ?);

update:
UPDATE friend SET name = ?, notes = ?, desired_frequency_days = ? WHERE id = ?;

deleteById:
DELETE FROM friend WHERE id = ?;
```

**Step 2: Create `ContactChannel.sq`**

File: `src/main/sqldelight/com/efetepe/amigos/data/ContactChannel.sq`
```sql
CREATE TABLE contact_channel (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    friend_id INTEGER NOT NULL REFERENCES friend(id) ON DELETE CASCADE,
    channel_type TEXT NOT NULL,
    address TEXT NOT NULL,
    preferred INTEGER NOT NULL DEFAULT 0
);

selectByFriendId:
SELECT * FROM contact_channel WHERE friend_id = ? ORDER BY preferred DESC;

insert:
INSERT INTO contact_channel (friend_id, channel_type, address, preferred)
VALUES (?, ?, ?, ?);

update:
UPDATE contact_channel SET channel_type = ?, address = ?, preferred = ? WHERE id = ?;

deleteById:
DELETE FROM contact_channel WHERE id = ?;

deleteByFriendId:
DELETE FROM contact_channel WHERE friend_id = ?;
```

**Step 3: Create `ContactLog.sq`**

File: `src/main/sqldelight/com/efetepe/amigos/data/ContactLog.sq`
```sql
CREATE TABLE contact_log (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    friend_id INTEGER NOT NULL REFERENCES friend(id) ON DELETE CASCADE,
    channel_type TEXT NOT NULL,
    prompted_at TEXT NOT NULL,
    contacted_at TEXT,
    skipped INTEGER NOT NULL DEFAULT 0
);

selectByFriendId:
SELECT * FROM contact_log WHERE friend_id = ? ORDER BY prompted_at DESC;

selectAll:
SELECT * FROM contact_log ORDER BY prompted_at DESC;

lastContactForFriend:
SELECT MAX(contacted_at) AS last_contacted FROM contact_log WHERE friend_id = ? AND contacted_at IS NOT NULL;

insert:
INSERT INTO contact_log (friend_id, channel_type, prompted_at, skipped)
VALUES (?, ?, ?, ?);

markContacted:
UPDATE contact_log SET contacted_at = ?, skipped = 0 WHERE id = ?;

markSkipped:
UPDATE contact_log SET skipped = 1 WHERE id = ?;
```

**Step 4: Create `AppSettings.sq`**

File: `src/main/sqldelight/com/efetepe/amigos/data/AppSettings.sq`
```sql
CREATE TABLE app_settings (
    key TEXT PRIMARY KEY,
    value TEXT NOT NULL
);

selectByKey:
SELECT value FROM app_settings WHERE key = ?;

upsert:
INSERT OR REPLACE INTO app_settings (key, value) VALUES (?, ?);

selectAll:
SELECT * FROM app_settings;
```

**Step 5: Create `DatabaseFactory.kt`**

File: `src/main/kotlin/com/efetepe/amigos/data/DatabaseFactory.kt`
```kotlin
package com.efetepe.amigos.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import java.io.File

object DatabaseFactory {
    private const val DB_NAME = "amigos.db"

    fun create(): AmigosDatabase {
        val dbDir = File(System.getProperty("user.home"), ".amigos")
        dbDir.mkdirs()
        val dbPath = File(dbDir, DB_NAME).absolutePath

        val driver = JdbcSqliteDriver("jdbc:sqlite:$dbPath")
        AmigosDatabase.Schema.create(driver)

        return AmigosDatabase(driver)
    }

    fun createInMemory(): AmigosDatabase {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        AmigosDatabase.Schema.create(driver)
        return AmigosDatabase(driver)
    }
}
```

**Step 6: Verify SQLDelight generates code**

Run: `./gradlew generateMainAmigosDatabase`
Expected: BUILD SUCCESSFUL, generated Kotlin code in `build/generated/sqldelight/`

**Step 7: Verify full build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 8: Commit**

```bash
git add src/main/sqldelight/ src/main/kotlin/com/efetepe/amigos/data/DatabaseFactory.kt
git commit -m "feat: add SQLDelight schema for friends, channels, log, and settings"
```

---

## Task 3: Data Layer — Domain Models & Repositories

Create Kotlin domain models and repository classes that wrap SQLDelight queries. Test-driven.

**Files:**
- Create: `src/main/kotlin/com/efetepe/amigos/data/models/ChannelType.kt`
- Create: `src/main/kotlin/com/efetepe/amigos/data/FriendRepository.kt`
- Create: `src/main/kotlin/com/efetepe/amigos/data/SettingsRepository.kt`
- Create: `src/test/kotlin/com/efetepe/amigos/data/FriendRepositoryTest.kt`
- Create: `src/test/kotlin/com/efetepe/amigos/data/SettingsRepositoryTest.kt`

**Step 1: Create `ChannelType.kt`**

File: `src/main/kotlin/com/efetepe/amigos/data/models/ChannelType.kt`
```kotlin
package com.efetepe.amigos.data.models

enum class ChannelType(val displayName: String, val deepLinkPrefix: String) {
    WHATSAPP("WhatsApp", "https://wa.me/"),
    IMESSAGE("iMessage", "imessage://"),
    EMAIL("Email", "mailto:"),
    SMS("SMS", "sms:"),
    TELEGRAM("Telegram", "https://t.me/");

    fun buildDeepLink(address: String): String = "$deepLinkPrefix$address"
}
```

**Step 2: Write failing test for `FriendRepository`**

File: `src/test/kotlin/com/efetepe/amigos/data/FriendRepositoryTest.kt`
```kotlin
package com.efetepe.amigos.data

import com.efetepe.amigos.data.models.ChannelType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class FriendRepositoryTest {
    private val db = DatabaseFactory.createInMemory()
    private val repo = FriendRepository(db)

    @Test
    fun `addFriend and getAllFriends returns the friend`() {
        repo.addFriend("Alice", "College friend", 14)
        val friends = repo.getAllFriends()
        assertEquals(1, friends.size)
        assertEquals("Alice", friends[0].name)
        assertEquals(14, friends[0].desired_frequency_days)
    }

    @Test
    fun `addChannel and getChannels returns channels for friend`() {
        repo.addFriend("Bob", null, 30)
        val bob = repo.getAllFriends().first()
        repo.addChannel(bob.id, ChannelType.WHATSAPP, "+1234567890", preferred = true)
        repo.addChannel(bob.id, ChannelType.EMAIL, "bob@example.com", preferred = false)

        val channels = repo.getChannels(bob.id)
        assertEquals(2, channels.size)
        assertEquals("WHATSAPP", channels[0].channel_type) // preferred first due to ORDER BY
    }

    @Test
    fun `logPrompt and markContacted records contact`() {
        repo.addFriend("Carol", null, 7)
        val carol = repo.getAllFriends().first()
        val logId = repo.logPrompt(carol.id, ChannelType.IMESSAGE)

        assertNotNull(logId)

        repo.markContacted(logId)
        val lastContact = repo.getLastContactDate(carol.id)
        assertNotNull(lastContact)
    }

    @Test
    fun `getLastContactDate returns null for never-contacted friend`() {
        repo.addFriend("Dave", null, 30)
        val dave = repo.getAllFriends().first()
        val lastContact = repo.getLastContactDate(dave.id)
        assertEquals(null, lastContact)
    }

    @Test
    fun `deleteFriend removes friend and cascaded data`() {
        repo.addFriend("Eve", null, 30)
        val eve = repo.getAllFriends().first()
        repo.addChannel(eve.id, ChannelType.SMS, "+999", preferred = true)
        repo.deleteFriend(eve.id)

        assertTrue(repo.getAllFriends().isEmpty())
    }
}
```

**Step 3: Run test to verify it fails**

Run: `./gradlew test --tests "com.efetepe.amigos.data.FriendRepositoryTest"`
Expected: FAIL — `FriendRepository` class not found.

**Step 4: Implement `FriendRepository`**

File: `src/main/kotlin/com/efetepe/amigos/data/FriendRepository.kt`
```kotlin
package com.efetepe.amigos.data

import com.efetepe.amigos.data.models.ChannelType
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class FriendRepository(private val db: AmigosDatabase) {

    fun addFriend(name: String, notes: String?, desiredFrequencyDays: Long) {
        db.friendQueries.insert(name, notes, desiredFrequencyDays)
    }

    fun getAllFriends() = db.friendQueries.selectAll().executeAsList()

    fun getFriend(id: Long) = db.friendQueries.selectById(id).executeAsOneOrNull()

    fun updateFriend(id: Long, name: String, notes: String?, desiredFrequencyDays: Long) {
        db.friendQueries.update(name, notes, desiredFrequencyDays, id)
    }

    fun deleteFriend(id: Long) {
        db.contactChannelQueries.deleteByFriendId(id)
        db.contactLogQueries.let { /* cascade handled by FK if enabled, or delete manually */ }
        db.friendQueries.deleteById(id)
    }

    fun addChannel(friendId: Long, type: ChannelType, address: String, preferred: Boolean) {
        db.contactChannelQueries.insert(friendId, type.name, address, if (preferred) 1L else 0L)
    }

    fun getChannels(friendId: Long) =
        db.contactChannelQueries.selectByFriendId(friendId).executeAsList()

    fun logPrompt(friendId: Long, channelType: ChannelType): Long {
        val now = Clock.System.now().toString()
        db.contactLogQueries.insert(friendId, channelType.name, now, 0L)
        // Get the last inserted rowid
        val logs = db.contactLogQueries.selectByFriendId(friendId).executeAsList()
        return logs.first().id
    }

    fun markContacted(logId: Long) {
        val now = Clock.System.now().toString()
        db.contactLogQueries.markContacted(now, logId)
    }

    fun markSkipped(logId: Long) {
        db.contactLogQueries.markSkipped(logId)
    }

    fun getLastContactDate(friendId: Long): Instant? {
        val result = db.contactLogQueries.lastContactForFriend(friendId).executeAsOneOrNull()
        return result?.last_contacted?.let { Instant.parse(it) }
    }

    fun getContactLog(friendId: Long) =
        db.contactLogQueries.selectByFriendId(friendId).executeAsList()

    fun getAllContactLogs() =
        db.contactLogQueries.selectAll().executeAsList()
}
```

Note: `kotlinx.datetime.Clock` was moved to `kotlin.time.Clock` in kotlinx-datetime 0.7+. Since we're using 0.6.1, `kotlinx.datetime.Clock` is correct. If version is bumped later, update to `kotlin.time.Clock.System`.

**Step 5: Run tests**

Run: `./gradlew test --tests "com.efetepe.amigos.data.FriendRepositoryTest"`
Expected: All 5 tests PASS. If SQLite FK cascade doesn't work in-memory, adjust `deleteFriend` to manually delete channels and logs first.

**Step 6: Write failing test for `SettingsRepository`**

File: `src/test/kotlin/com/efetepe/amigos/data/SettingsRepositoryTest.kt`
```kotlin
package com.efetepe.amigos.data

import kotlin.test.Test
import kotlin.test.assertEquals

class SettingsRepositoryTest {
    private val db = DatabaseFactory.createInMemory()
    private val repo = SettingsRepository(db)

    @Test
    fun `get returns default when key not set`() {
        assertEquals(3, repo.nudgesPerWeek)
    }

    @Test
    fun `set and get nudgesPerWeek`() {
        repo.nudgesPerWeek = 5
        assertEquals(5, repo.nudgesPerWeek)
    }

    @Test
    fun `set and get activeHours`() {
        repo.activeHoursStart = "10:00"
        repo.activeHoursEnd = "22:00"
        assertEquals("10:00", repo.activeHoursStart)
        assertEquals("22:00", repo.activeHoursEnd)
    }

    @Test
    fun `set and get notificationDays`() {
        repo.notificationDays = listOf("MON", "TUE", "THU")
        assertEquals(listOf("MON", "TUE", "THU"), repo.notificationDays)
    }
}
```

**Step 7: Run test to verify it fails**

Run: `./gradlew test --tests "com.efetepe.amigos.data.SettingsRepositoryTest"`
Expected: FAIL — `SettingsRepository` not found.

**Step 8: Implement `SettingsRepository`**

File: `src/main/kotlin/com/efetepe/amigos/data/SettingsRepository.kt`
```kotlin
package com.efetepe.amigos.data

class SettingsRepository(private val db: AmigosDatabase) {

    private fun get(key: String, default: String): String =
        db.appSettingsQueries.selectByKey(key).executeAsOneOrNull()?.let { it } ?: default

    private fun set(key: String, value: String) {
        db.appSettingsQueries.upsert(key, value)
    }

    var nudgesPerWeek: Int
        get() = get("nudges_per_week", "3").toInt()
        set(value) = set("nudges_per_week", value.toString())

    var activeHoursStart: String
        get() = get("active_hours_start", "09:00")
        set(value) = set("active_hours_start", value)

    var activeHoursEnd: String
        get() = get("active_hours_end", "21:00")
        set(value) = set("active_hours_end", value)

    var notificationDays: List<String>
        get() = get("notification_days", "MON,WED,FRI").split(",")
        set(value) = set("notification_days", value.joinToString(","))
}
```

**Step 9: Run tests**

Run: `./gradlew test --tests "com.efetepe.amigos.data.SettingsRepositoryTest"`
Expected: All 4 tests PASS.

**Step 10: Commit**

```bash
git add src/
git commit -m "feat: add domain models, FriendRepository and SettingsRepository with tests"
```

---

## Task 4: Staleness Scorer & Friend Selection

The core scheduling algorithm. Pure functions, highly testable.

**Files:**
- Create: `src/main/kotlin/com/efetepe/amigos/scheduler/StalenessScorer.kt`
- Create: `src/test/kotlin/com/efetepe/amigos/scheduler/StalenessScorerTest.kt`

**Step 1: Write failing tests**

File: `src/test/kotlin/com/efetepe/amigos/scheduler/StalenessScorerTest.kt`
```kotlin
package com.efetepe.amigos.scheduler

import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.time.Duration.Companion.days

class StalenessScorerTest {

    @Test
    fun `never contacted friend has max staleness`() {
        val score = StalenessScorer.score(
            lastContacted = null,
            desiredFrequencyDays = 30,
            now = Clock.System.now()
        )
        assertEquals(Double.MAX_VALUE, score)
    }

    @Test
    fun `friend contacted today has zero staleness`() {
        val now = Clock.System.now()
        val score = StalenessScorer.score(
            lastContacted = now,
            desiredFrequencyDays = 30,
            now = now
        )
        assertEquals(0.0, score, 0.01)
    }

    @Test
    fun `friend contacted 15 days ago with 30-day frequency is 0_5 stale`() {
        val now = Clock.System.now()
        val score = StalenessScorer.score(
            lastContacted = now - 15.days,
            desiredFrequencyDays = 30,
            now = now
        )
        assertEquals(0.5, score, 0.01)
    }

    @Test
    fun `friend contacted 30 days ago with 30-day frequency is 1_0 stale`() {
        val now = Clock.System.now()
        val score = StalenessScorer.score(
            lastContacted = now - 30.days,
            desiredFrequencyDays = 30,
            now = now
        )
        assertEquals(1.0, score, 0.01)
    }

    @Test
    fun `pickFriend returns null when no friends are due`() {
        val now = Clock.System.now()
        val candidates = listOf(
            FriendCandidate(id = 1, name = "Alice", lastContacted = now, desiredFrequencyDays = 30)
        )
        val picked = StalenessScorer.pickFriend(candidates, lastPickedId = null, now = now)
        assertNull(picked)
    }

    @Test
    fun `pickFriend returns overdue friend`() {
        val now = Clock.System.now()
        val candidates = listOf(
            FriendCandidate(id = 1, name = "Alice", lastContacted = now - 60.days, desiredFrequencyDays = 30),
            FriendCandidate(id = 2, name = "Bob", lastContacted = now, desiredFrequencyDays = 30)
        )
        val picked = StalenessScorer.pickFriend(candidates, lastPickedId = null, now = now)
        assertNotNull(picked)
        assertEquals(1L, picked.id)
    }

    @Test
    fun `pickFriend avoids last picked friend when alternatives exist`() {
        val now = Clock.System.now()
        val candidates = listOf(
            FriendCandidate(id = 1, name = "Alice", lastContacted = now - 60.days, desiredFrequencyDays = 30),
            FriendCandidate(id = 2, name = "Bob", lastContacted = now - 45.days, desiredFrequencyDays = 30)
        )
        val picked = StalenessScorer.pickFriend(candidates, lastPickedId = 1L, now = now)
        assertNotNull(picked)
        assertEquals(2L, picked.id)
    }

    @Test
    fun `pickFriend picks last picked if they are the only one due`() {
        val now = Clock.System.now()
        val candidates = listOf(
            FriendCandidate(id = 1, name = "Alice", lastContacted = now - 60.days, desiredFrequencyDays = 30),
            FriendCandidate(id = 2, name = "Bob", lastContacted = now, desiredFrequencyDays = 30)
        )
        val picked = StalenessScorer.pickFriend(candidates, lastPickedId = 1L, now = now)
        assertNotNull(picked)
        assertEquals(1L, picked.id)
    }
}
```

**Step 2: Run tests to verify they fail**

Run: `./gradlew test --tests "com.efetepe.amigos.scheduler.StalenessScorerTest"`
Expected: FAIL — classes not found.

**Step 3: Implement `StalenessScorer`**

File: `src/main/kotlin/com/efetepe/amigos/scheduler/StalenessScorer.kt`
```kotlin
package com.efetepe.amigos.scheduler

import kotlinx.datetime.Instant
import kotlin.random.Random
import kotlin.time.Duration.Companion.days

data class FriendCandidate(
    val id: Long,
    val name: String,
    val lastContacted: Instant?,
    val desiredFrequencyDays: Long
)

object StalenessScorer {
    private const val THRESHOLD = 0.8

    fun score(lastContacted: Instant?, desiredFrequencyDays: Long, now: Instant): Double {
        if (lastContacted == null) return Double.MAX_VALUE
        val daysSince = (now - lastContacted).inWholeDays.toDouble()
        return daysSince / desiredFrequencyDays.toDouble()
    }

    fun pickFriend(
        candidates: List<FriendCandidate>,
        lastPickedId: Long?,
        now: Instant,
        random: Random = Random
    ): FriendCandidate? {
        val scored = candidates.map { it to score(it.lastContacted, it.desiredFrequencyDays, now) }
        val due = scored.filter { it.second >= THRESHOLD }

        if (due.isEmpty()) return null

        // Try to avoid picking the same friend consecutively
        val filtered = due.filter { it.first.id != lastPickedId }
        val pool = filtered.ifEmpty { due }

        // Weighted random selection by staleness score
        val totalWeight = pool.sumOf { it.second }
        var roll = random.nextDouble() * totalWeight
        for ((candidate, weight) in pool) {
            roll -= weight
            if (roll <= 0) return candidate
        }
        return pool.last().first
    }
}
```

**Step 4: Run tests**

Run: `./gradlew test --tests "com.efetepe.amigos.scheduler.StalenessScorerTest"`
Expected: All 8 tests PASS.

**Step 5: Commit**

```bash
git add src/
git commit -m "feat: add staleness scorer with weighted random friend selection"
```

---

## Task 5: macOS Notification Bridge

Send native macOS notifications from JVM via osascript.

**Files:**
- Create: `src/main/kotlin/com/efetepe/amigos/notification/MacNotifier.kt`
- Create: `src/test/kotlin/com/efetepe/amigos/notification/MacNotifierTest.kt`

**Step 1: Write a basic test**

File: `src/test/kotlin/com/efetepe/amigos/notification/MacNotifierTest.kt`
```kotlin
package com.efetepe.amigos.notification

import kotlin.test.Test
import kotlin.test.assertTrue

class MacNotifierTest {
    @Test
    fun `buildScript generates valid osascript command`() {
        val script = MacNotifier.buildAppleScript(
            title = "Amigos",
            message = "Time to reach out to Alice via WhatsApp",
            soundName = "default"
        )
        assertTrue(script.contains("display notification"))
        assertTrue(script.contains("Alice"))
        assertTrue(script.contains("Amigos"))
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.efetepe.amigos.notification.MacNotifierTest"`
Expected: FAIL — class not found.

**Step 3: Implement `MacNotifier`**

File: `src/main/kotlin/com/efetepe/amigos/notification/MacNotifier.kt`
```kotlin
package com.efetepe.amigos.notification

object MacNotifier {

    fun buildAppleScript(title: String, message: String, soundName: String = "default"): String {
        val escapedTitle = title.replace("\"", "\\\"")
        val escapedMessage = message.replace("\"", "\\\"")
        return """display notification "$escapedMessage" with title "$escapedTitle" sound name "$soundName""""
    }

    fun notify(title: String, message: String) {
        val script = buildAppleScript(title, message)
        ProcessBuilder("osascript", "-e", script)
            .redirectErrorStream(true)
            .start()
            .waitFor()
    }
}
```

**Step 4: Run test**

Run: `./gradlew test --tests "com.efetepe.amigos.notification.MacNotifierTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add src/
git commit -m "feat: add macOS notification bridge via osascript"
```

---

## Task 6: Scheduler — Background Coroutine Loop

The scheduler that ties staleness scoring, notifications, and timing together.

**Files:**
- Create: `src/main/kotlin/com/efetepe/amigos/scheduler/Scheduler.kt`
- Create: `src/test/kotlin/com/efetepe/amigos/scheduler/SchedulerTest.kt`

**Step 1: Write failing test**

File: `src/test/kotlin/com/efetepe/amigos/scheduler/SchedulerTest.kt`
```kotlin
package com.efetepe.amigos.scheduler

import com.efetepe.amigos.data.DatabaseFactory
import com.efetepe.amigos.data.FriendRepository
import com.efetepe.amigos.data.SettingsRepository
import com.efetepe.amigos.data.models.ChannelType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SchedulerTest {
    private val db = DatabaseFactory.createInMemory()
    private val friendRepo = FriendRepository(db)
    private val settingsRepo = SettingsRepository(db)

    @Test
    fun `buildCandidates returns all friends with their staleness data`() {
        friendRepo.addFriend("Alice", null, 14)
        friendRepo.addFriend("Bob", null, 30)
        val alice = friendRepo.getAllFriends().first { it.name == "Alice" }
        friendRepo.addChannel(alice.id, ChannelType.WHATSAPP, "+1234", preferred = true)

        val candidates = Scheduler.buildCandidates(friendRepo)
        assertEquals(2, candidates.size)
    }

    @Test
    fun `getPreferredChannel returns preferred channel`() {
        friendRepo.addFriend("Alice", null, 14)
        val alice = friendRepo.getAllFriends().first()
        friendRepo.addChannel(alice.id, ChannelType.EMAIL, "a@b.com", preferred = false)
        friendRepo.addChannel(alice.id, ChannelType.WHATSAPP, "+1234", preferred = true)

        val channel = Scheduler.getPreferredChannel(friendRepo, alice.id)
        assertNotNull(channel)
        assertEquals("WHATSAPP", channel.channel_type)
    }
}
```

**Step 2: Run test to verify it fails**

Run: `./gradlew test --tests "com.efetepe.amigos.scheduler.SchedulerTest"`
Expected: FAIL — `Scheduler` not found.

**Step 3: Implement `Scheduler`**

File: `src/main/kotlin/com/efetepe/amigos/scheduler/Scheduler.kt`
```kotlin
package com.efetepe.amigos.scheduler

import com.efetepe.amigos.data.FriendRepository
import com.efetepe.amigos.data.SettingsRepository
import com.efetepe.amigos.data.models.ChannelType
import com.efetepe.amigos.notification.MacNotifier
import kotlinx.coroutines.*
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.random.Random
import kotlin.time.Duration.Companion.minutes

data class NudgeResult(
    val friendId: Long,
    val friendName: String,
    val channelType: ChannelType,
    val address: String,
    val logId: Long
)

class Scheduler(
    private val friendRepo: FriendRepository,
    private val settingsRepo: SettingsRepository,
    private val onNudge: (NudgeResult) -> Unit
) {
    private var lastPickedId: Long? = null
    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        job = scope.launch {
            while (isActive) {
                checkAndNudge()
                delay(15.minutes) // Check every 15 minutes
            }
        }
    }

    fun stop() {
        job?.cancel()
    }

    internal fun checkAndNudge() {
        val now = Clock.System.now()
        if (!isWithinSchedule(now)) return

        val candidates = buildCandidates(friendRepo)
        val picked = StalenessScorer.pickFriend(candidates, lastPickedId, now) ?: return
        val channel = getPreferredChannel(friendRepo, picked.id) ?: return
        val channelType = ChannelType.valueOf(channel.channel_type)

        val logId = friendRepo.logPrompt(picked.id, channelType)
        lastPickedId = picked.id

        MacNotifier.notify(
            title = "Amigos",
            message = "Time to reach out to ${picked.name} via ${channelType.displayName}"
        )

        onNudge(NudgeResult(picked.id, picked.name, channelType, channel.address, logId))
    }

    private fun isWithinSchedule(now: Instant): Boolean {
        val tz = TimeZone.currentSystemDefault()
        val localNow = now.toLocalDateTime(tz)
        val dayName = localNow.dayOfWeek.name.take(3) // MON, TUE, etc.

        if (dayName !in settingsRepo.notificationDays) return false

        val startTime = LocalTime.parse(settingsRepo.activeHoursStart)
        val endTime = LocalTime.parse(settingsRepo.activeHoursEnd)
        return localNow.time in startTime..endTime
    }

    companion object {
        fun buildCandidates(friendRepo: FriendRepository): List<FriendCandidate> {
            return friendRepo.getAllFriends().map { friend ->
                FriendCandidate(
                    id = friend.id,
                    name = friend.name,
                    lastContacted = friendRepo.getLastContactDate(friend.id),
                    desiredFrequencyDays = friend.desired_frequency_days
                )
            }
        }

        fun getPreferredChannel(friendRepo: FriendRepository, friendId: Long) =
            friendRepo.getChannels(friendId).firstOrNull()
    }
}
```

**Step 4: Run tests**

Run: `./gradlew test --tests "com.efetepe.amigos.scheduler.SchedulerTest"`
Expected: PASS.

**Step 5: Commit**

```bash
git add src/
git commit -m "feat: add background scheduler with schedule-aware nudging"
```

---

## Task 7: UI — Action Window (The Core Popup)

The small Compose window that appears when a nudge fires. Shows friend info, open-app button, done/snooze/skip.

**Files:**
- Create: `src/main/kotlin/com/efetepe/amigos/ui/ActionWindow.kt`
- Create: `src/main/kotlin/com/efetepe/amigos/ui/DeepLinker.kt`

**Step 1: Create `DeepLinker.kt`**

File: `src/main/kotlin/com/efetepe/amigos/ui/DeepLinker.kt`
```kotlin
package com.efetepe.amigos.ui

import com.efetepe.amigos.data.models.ChannelType
import java.awt.Desktop
import java.net.URI

object DeepLinker {
    fun open(channelType: ChannelType, address: String) {
        val uri = URI(channelType.buildDeepLink(address))
        if (Desktop.isDesktopSupported()) {
            Desktop.getDesktop().browse(uri)
        }
    }
}
```

**Step 2: Create `ActionWindow.kt`**

File: `src/main/kotlin/com/efetepe/amigos/ui/ActionWindow.kt`
```kotlin
package com.efetepe.amigos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.efetepe.amigos.data.models.ChannelType

data class ActionWindowState(
    val friendName: String,
    val friendNotes: String?,
    val channelType: ChannelType,
    val address: String,
    val logId: Long
)

@Composable
fun ActionWindowContent(
    state: ActionWindowState,
    onOpenApp: () -> Unit,
    onDone: () -> Unit,
    onSnooze: () -> Unit,
    onSkip: () -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Time to reach out!",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )

            Text(
                text = state.friendName,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )

            if (!state.friendNotes.isNullOrBlank()) {
                Text(
                    text = state.friendNotes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = onOpenApp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open ${state.channelType.displayName}")
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = onSkip, modifier = Modifier.weight(1f)) {
                    Text("Skip")
                }
                OutlinedButton(onClick = onSnooze, modifier = Modifier.weight(1f)) {
                    Text("Snooze")
                }
                Button(onClick = onDone, modifier = Modifier.weight(1f)) {
                    Text("Done")
                }
            }
        }
    }
}
```

**Step 3: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL.

**Step 4: Commit**

```bash
git add src/
git commit -m "feat: add action window UI and deep-link opener"
```

---

## Task 8: UI — Add/Edit Friend Dialog

Dialog for adding and editing friends with their contact channels.

**Files:**
- Create: `src/main/kotlin/com/efetepe/amigos/ui/AddFriendDialog.kt`

**Step 1: Create `AddFriendDialog.kt`**

File: `src/main/kotlin/com/efetepe/amigos/ui/AddFriendDialog.kt`
```kotlin
package com.efetepe.amigos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.efetepe.amigos.data.models.ChannelType

data class ChannelInput(
    val type: ChannelType = ChannelType.WHATSAPP,
    val address: String = "",
    val preferred: Boolean = false
)

@Composable
fun AddFriendDialogContent(
    initialName: String = "",
    initialNotes: String = "",
    initialFrequency: Int = 30,
    initialChannels: List<ChannelInput> = listOf(ChannelInput()),
    onSave: (name: String, notes: String?, frequency: Int, channels: List<ChannelInput>) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf(initialName) }
    var notes by remember { mutableStateOf(initialNotes) }
    var frequency by remember { mutableStateOf(initialFrequency.toString()) }
    var channels by remember { mutableStateOf(initialChannels) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Add Friend", style = MaterialTheme.typography.titleLarge)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Name") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes (optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = frequency,
                onValueChange = { frequency = it.filter { ch -> ch.isDigit() } },
                label = { Text("Contact every N days") },
                modifier = Modifier.fillMaxWidth()
            )

            Text("Contact Channels", style = MaterialTheme.typography.titleSmall)

            channels.forEachIndexed { index, channel ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    var expanded by remember { mutableStateOf(false) }
                    Box(modifier = Modifier.weight(0.35f)) {
                        OutlinedButton(onClick = { expanded = true }, modifier = Modifier.fillMaxWidth()) {
                            Text(channel.type.displayName)
                        }
                        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
                            ChannelType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.displayName) },
                                    onClick = {
                                        channels = channels.toMutableList().apply {
                                            this[index] = channel.copy(type = type)
                                        }
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                    OutlinedTextField(
                        value = channel.address,
                        onValueChange = { newAddr ->
                            channels = channels.toMutableList().apply {
                                this[index] = channel.copy(address = newAddr)
                            }
                        },
                        label = { Text("Address") },
                        modifier = Modifier.weight(0.65f)
                    )
                }
            }

            TextButton(onClick = { channels = channels + ChannelInput() }) {
                Text("+ Add Channel")
            }

            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedButton(onClick = onCancel, modifier = Modifier.weight(1f)) {
                    Text("Cancel")
                }
                Button(
                    onClick = {
                        val freq = frequency.toIntOrNull() ?: 30
                        val validChannels = channels.filter { it.address.isNotBlank() }
                            .mapIndexed { i, c -> if (i == 0) c.copy(preferred = true) else c }
                        onSave(name, notes.ifBlank { null }, freq, validChannels)
                    },
                    modifier = Modifier.weight(1f),
                    enabled = name.isNotBlank() && channels.any { it.address.isNotBlank() }
                ) {
                    Text("Save")
                }
            }
        }
    }
}
```

**Step 2: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL.

**Step 3: Commit**

```bash
git add src/
git commit -m "feat: add friend dialog with channel management"
```

---

## Task 9: UI — Contact Log View & Settings View

**Files:**
- Create: `src/main/kotlin/com/efetepe/amigos/ui/LogView.kt`
- Create: `src/main/kotlin/com/efetepe/amigos/ui/SettingsView.kt`

**Step 1: Create `LogView.kt`**

File: `src/main/kotlin/com/efetepe/amigos/ui/LogView.kt`
```kotlin
package com.efetepe.amigos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class LogEntry(
    val friendName: String,
    val channelType: String,
    val promptedAt: String,
    val contactedAt: String?,
    val skipped: Boolean
)

@Composable
fun LogViewContent(entries: List<LogEntry>, onClose: () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(24.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Contact Log", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onClose) { Text("Close") }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (entries.isEmpty()) {
                Text("No contact history yet.", style = MaterialTheme.typography.bodyMedium)
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(entries) { entry ->
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(entry.friendName, style = MaterialTheme.typography.titleSmall)
                                Text("via ${entry.channelType} — ${entry.promptedAt}")
                                Text(
                                    when {
                                        entry.skipped -> "Skipped"
                                        entry.contactedAt != null -> "Contacted: ${entry.contactedAt}"
                                        else -> "Pending"
                                    },
                                    color = when {
                                        entry.skipped -> MaterialTheme.colorScheme.error
                                        entry.contactedAt != null -> MaterialTheme.colorScheme.primary
                                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

**Step 2: Create `SettingsView.kt`**

File: `src/main/kotlin/com/efetepe/amigos/ui/SettingsView.kt`
```kotlin
package com.efetepe.amigos.ui

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun SettingsViewContent(
    nudgesPerWeek: Int,
    activeStart: String,
    activeEnd: String,
    notificationDays: List<String>,
    onSave: (nudges: Int, start: String, end: String, days: List<String>) -> Unit,
    onClose: () -> Unit
) {
    var nudges by remember { mutableStateOf(nudgesPerWeek.toString()) }
    var start by remember { mutableStateOf(activeStart) }
    var end by remember { mutableStateOf(activeEnd) }
    val allDays = listOf("MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN")
    var selectedDays by remember { mutableStateOf(notificationDays.toSet()) }

    Surface(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Settings", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = onClose) { Text("Close") }
            }

            OutlinedTextField(
                value = nudges,
                onValueChange = { nudges = it.filter { ch -> ch.isDigit() } },
                label = { Text("Nudges per week") },
                modifier = Modifier.fillMaxWidth()
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = start,
                    onValueChange = { start = it },
                    label = { Text("Active hours start") },
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = end,
                    onValueChange = { end = it },
                    label = { Text("Active hours end") },
                    modifier = Modifier.weight(1f)
                )
            }

            Text("Notification days", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                allDays.forEach { day ->
                    FilterChip(
                        selected = day in selectedDays,
                        onClick = {
                            selectedDays = if (day in selectedDays) selectedDays - day else selectedDays + day
                        },
                        label = { Text(day) }
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = {
                    onSave(nudges.toIntOrNull() ?: 3, start, end, selectedDays.toList().sorted())
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Save")
            }
        }
    }
}
```

**Step 3: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL.

**Step 4: Commit**

```bash
git add src/
git commit -m "feat: add contact log view and settings view"
```

---

## Task 10: Main Entry Point — Wire Everything Together

Connect tray icon, scheduler, and all windows into the main application.

**Files:**
- Modify: `src/main/kotlin/com/efetepe/amigos/Main.kt`

**Step 1: Rewrite `Main.kt`**

File: `src/main/kotlin/com/efetepe/amigos/Main.kt`
```kotlin
package com.efetepe.amigos

import androidx.compose.runtime.*
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.window.*
import com.efetepe.amigos.data.DatabaseFactory
import com.efetepe.amigos.data.FriendRepository
import com.efetepe.amigos.data.SettingsRepository
import com.efetepe.amigos.data.models.ChannelType
import com.efetepe.amigos.scheduler.NudgeResult
import com.efetepe.amigos.scheduler.Scheduler
import com.efetepe.amigos.ui.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

enum class AppScreen { NONE, ACTION, ADD_FRIEND, LOG, SETTINGS }

fun main() = application {
    val db = remember { DatabaseFactory.create() }
    val friendRepo = remember { FriendRepository(db) }
    val settingsRepo = remember { SettingsRepository(db) }

    var currentScreen by remember { mutableStateOf(AppScreen.NONE) }
    var currentNudge by remember { mutableStateOf<NudgeResult?>(null) }
    var editingFriendId by remember { mutableStateOf<Long?>(null) }

    val scheduler = remember {
        Scheduler(friendRepo, settingsRepo) { nudge ->
            currentNudge = nudge
            currentScreen = AppScreen.ACTION
        }
    }

    LaunchedEffect(Unit) {
        scheduler.start(CoroutineScope(Dispatchers.Default + SupervisorJob()))
    }

    Tray(
        icon = TrayIcon,
        tooltip = "Amigos",
        onAction = { currentScreen = AppScreen.ACTION },
        menu = {
            Item("Add Friend") { currentScreen = AppScreen.ADD_FRIEND }
            Item("View Log") { currentScreen = AppScreen.LOG }
            Item("Settings") { currentScreen = AppScreen.SETTINGS }
            Separator()
            Item("Quit") { scheduler.stop(); exitApplication() }
        }
    )

    when (currentScreen) {
        AppScreen.ACTION -> {
            val nudge = currentNudge
            if (nudge != null) {
                Window(
                    onCloseRequest = { currentScreen = AppScreen.NONE },
                    title = "Amigos — Reach Out",
                    state = rememberWindowState(width = 400.dp, height = 350.dp),
                    resizable = false
                ) {
                    val friend = friendRepo.getFriend(nudge.friendId)
                    ActionWindowContent(
                        state = ActionWindowState(
                            friendName = nudge.friendName,
                            friendNotes = friend?.notes,
                            channelType = nudge.channelType,
                            address = nudge.address,
                            logId = nudge.logId
                        ),
                        onOpenApp = { DeepLinker.open(nudge.channelType, nudge.address) },
                        onDone = {
                            friendRepo.markContacted(nudge.logId)
                            currentScreen = AppScreen.NONE
                        },
                        onSnooze = { currentScreen = AppScreen.NONE },
                        onSkip = {
                            friendRepo.markSkipped(nudge.logId)
                            currentScreen = AppScreen.NONE
                        }
                    )
                }
            }
        }

        AppScreen.ADD_FRIEND -> {
            Window(
                onCloseRequest = { currentScreen = AppScreen.NONE; editingFriendId = null },
                title = "Amigos — Add Friend",
                state = rememberWindowState(width = 500.dp, height = 600.dp)
            ) {
                AddFriendDialogContent(
                    onSave = { name, notes, frequency, channels ->
                        friendRepo.addFriend(name, notes, frequency.toLong())
                        val friend = friendRepo.getAllFriends().last()
                        channels.forEach { ch ->
                            friendRepo.addChannel(friend.id, ch.type, ch.address, ch.preferred)
                        }
                        currentScreen = AppScreen.NONE
                    },
                    onCancel = { currentScreen = AppScreen.NONE }
                )
            }
        }

        AppScreen.LOG -> {
            Window(
                onCloseRequest = { currentScreen = AppScreen.NONE },
                title = "Amigos — Contact Log",
                state = rememberWindowState(width = 600.dp, height = 500.dp)
            ) {
                val logs = friendRepo.getAllContactLogs()
                val entries = logs.map { log ->
                    val friend = friendRepo.getFriend(log.friend_id)
                    LogEntry(
                        friendName = friend?.name ?: "Unknown",
                        channelType = log.channel_type,
                        promptedAt = log.prompted_at,
                        contactedAt = log.contacted_at,
                        skipped = log.skipped != 0L
                    )
                }
                LogViewContent(entries = entries, onClose = { currentScreen = AppScreen.NONE })
            }
        }

        AppScreen.SETTINGS -> {
            Window(
                onCloseRequest = { currentScreen = AppScreen.NONE },
                title = "Amigos — Settings",
                state = rememberWindowState(width = 500.dp, height = 500.dp)
            ) {
                SettingsViewContent(
                    nudgesPerWeek = settingsRepo.nudgesPerWeek,
                    activeStart = settingsRepo.activeHoursStart,
                    activeEnd = settingsRepo.activeHoursEnd,
                    notificationDays = settingsRepo.notificationDays,
                    onSave = { nudges, start, end, days ->
                        settingsRepo.nudgesPerWeek = nudges
                        settingsRepo.activeHoursStart = start
                        settingsRepo.activeHoursEnd = end
                        settingsRepo.notificationDays = days
                        currentScreen = AppScreen.NONE
                    },
                    onClose = { currentScreen = AppScreen.NONE }
                )
            }
        }

        AppScreen.NONE -> { /* No window shown, app lives in tray */ }
    }
}

// Placeholder tray icon — a simple colored circle
private val TrayIcon: java.awt.image.BufferedImage by lazy {
    java.awt.image.BufferedImage(32, 32, java.awt.image.BufferedImage.TYPE_INT_ARGB).apply {
        val g = createGraphics()
        g.color = java.awt.Color(0x6750A4) // Material purple
        g.fillOval(4, 4, 24, 24)
        g.dispose()
    }
}
```

Note: The `Tray` composable expects a `java.awt.image.BufferedImage` for the icon. We create a simple colored circle as placeholder. The `dp` imports for window size come from `androidx.compose.ui.unit.dp`. You'll need to add the import.

**Step 2: Add missing import**

Make sure `Main.kt` has: `import androidx.compose.ui.unit.dp`

**Step 3: Verify build**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL. May need minor adjustments to imports or Tray API based on exact Compose version.

**Step 4: Test the app manually**

Run: `./gradlew run`
Expected: App appears in system tray. Right-click shows menu. "Add Friend" opens dialog. "Settings" opens settings.

**Step 5: Commit**

```bash
git add src/
git commit -m "feat: wire tray icon, scheduler, and all windows into main app"
```

---

## Task 11: Manual Integration Test & Polish

Add a friend, verify notification fires, test the action flow end-to-end.

**Step 1: Run the app**

Run: `./gradlew run`

**Step 2: Add a test friend**

Right-click tray → "Add Friend" → enter:
- Name: "Test Friend"
- Notes: "Testing amigos"
- Frequency: 1 day
- Channel: WhatsApp, +1234567890

**Step 3: Verify the scheduler fires**

The scheduler checks every 15 minutes. For testing, temporarily change `delay(15.minutes)` to `delay(30.seconds)` in `Scheduler.kt`. Since the friend has never been contacted, they should be picked immediately on next check.

**Step 4: Verify notification appears**

Expected: macOS notification "Time to reach out to Test Friend via WhatsApp"

**Step 5: Verify action window works**

Click on tray icon or wait for notification. Action window should show friend name, "Open WhatsApp" button, and Done/Snooze/Skip buttons.

**Step 6: Restore scheduler delay**

Change `delay(30.seconds)` back to `delay(15.minutes)` in `Scheduler.kt`.

**Step 7: Commit final state**

```bash
git add -A
git commit -m "chore: integration test complete, app working end-to-end"
```

---

## Summary

| Task | Description | Tests |
|------|------------|-------|
| 1 | Project scaffolding | Manual (build + run) |
| 2 | SQLDelight schema + DB setup | Code generation verification |
| 3 | Domain models + repositories | 9 unit tests |
| 4 | Staleness scorer + friend selection | 8 unit tests |
| 5 | macOS notification bridge | 1 unit test |
| 6 | Background scheduler | 2 unit tests |
| 7 | Action window UI | Manual |
| 8 | Add friend dialog UI | Manual |
| 9 | Log view + Settings view UI | Manual |
| 10 | Main entry point — wire everything | Manual |
| 11 | Integration test + polish | Manual E2E |
