# macOS App Packaging Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Package Amigos as a proper macOS `.app` in a `.dmg`, with tray-only mode and auto-launch on login.

**Architecture:** Use the existing Compose Desktop `packageDmg` Gradle task (which wraps `jpackage`). Move the user-provided `.icns` into the build tree, configure LSUIElement for tray-only mode, and add a LaunchAgent installer that runs on first launch.

**Tech Stack:** Compose Desktop 1.7.3 packaging DSL, macOS LaunchAgent plist, `jpackage` (bundled with JDK)

---

### Task 1: Move icon into build tree and configure Gradle

**Files:**
- Move: `HAL9000.icns` → `src/main/packaging/icon.icns`
- Modify: `build.gradle.kts:37-49`

**Step 1: Move the icon file**

```bash
mkdir -p src/main/packaging
mv "HAL9000.icns" src/main/packaging/icon.icns
```

**Step 2: Update Gradle packaging config**

Replace the `compose.desktop` block in `build.gradle.kts` (lines 37-49) with:

```kotlin
compose.desktop {
    application {
        mainClass = "com.efetepe.amigos.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Dmg)
            packageName = "Amigos"
            packageVersion = "1.0.0"
            macOS {
                bundleID = "com.efetepe.amigos"
                dockName = "Amigos"
                iconFile.set(project.file("src/main/packaging/icon.icns"))
                infoPlist {
                    extraKeysRawXml = """
                        <key>LSUIElement</key>
                        <true/>
                    """.trimIndent()
                }
            }
        }
    }
}
```

Key additions:
- `iconFile` — points to the `.icns` so the `.app` bundle and `.dmg` use the HAL 9000 icon
- `dockName` — display name in Dock (when briefly visible)
- `LSUIElement = true` — hides from Dock and Cmd+Tab; app lives only in the tray

**Step 3: Verify it builds**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/packaging/icon.icns build.gradle.kts
git commit -m "feat: configure macOS app icon and tray-only mode (LSUIElement)"
```

---

### Task 2: Remove the setDockIcon() workaround

Since the app will now be tray-only (LSUIElement), the dock icon workaround in `Main.kt` is no longer needed — the app won't appear in the Dock at all.

**Files:**
- Modify: `src/main/kotlin/com/efetepe/amigos/Main.kt:18-20,102-110`

**Step 1: Remove the `setDockIcon()` call and function**

In `Main.kt`:
- Remove the `setDockIcon()` call on line 20
- Remove the entire `setDockIcon()` function (lines 102-110)

**Step 2: Verify it builds**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 3: Commit**

```bash
git add src/main/kotlin/com/efetepe/amigos/Main.kt
git commit -m "refactor: remove dock icon workaround (now tray-only via LSUIElement)"
```

---

### Task 3: Add LaunchAgent installer

**Files:**
- Create: `src/main/kotlin/com/efetepe/amigos/LaunchAgentInstaller.kt`
- Modify: `src/main/kotlin/com/efetepe/amigos/Main.kt`

**Step 1: Create `LaunchAgentInstaller.kt`**

```kotlin
package com.efetepe.amigos

import java.io.File

object LaunchAgentInstaller {
    private const val LABEL = "com.efetepe.amigos"
    private val plistFile = File(
        System.getProperty("user.home"),
        "Library/LaunchAgents/$LABEL.plist"
    )

    fun installIfNeeded() {
        if (plistFile.exists()) return

        val appPath = resolveAppPath() ?: return
        val plistContent = """
            <?xml version="1.0" encoding="UTF-8"?>
            <!DOCTYPE plist PUBLIC "-//Apple//DTD PLIST 1.0//EN"
              "http://www.apple.com/DTDs/PropertyList-1.0.dtd">
            <plist version="1.0">
            <dict>
                <key>Label</key>
                <string>$LABEL</string>
                <key>ProgramArguments</key>
                <array>
                    <string>open</string>
                    <string>-a</string>
                    <string>$appPath</string>
                </array>
                <key>RunAtLoad</key>
                <true/>
                <key>KeepAlive</key>
                <false/>
            </dict>
            </plist>
        """.trimIndent()

        plistFile.parentFile.mkdirs()
        plistFile.writeText(plistContent)
    }

    /**
     * Resolve the path to the .app bundle.
     * When running from a packaged app, the working directory or java.home
     * will be inside /Applications/Amigos.app/...
     * Returns null if not running from a packaged .app (e.g. during development).
     */
    private fun resolveAppPath(): String? {
        val javaHome = System.getProperty("java.home") ?: return null
        val appSuffix = ".app"
        val idx = javaHome.indexOf(appSuffix)
        if (idx == -1) return null
        return javaHome.substring(0, idx + appSuffix.length)
    }
}
```

Notes:
- Uses `open -a` in ProgramArguments — this is the standard way to launch a `.app` bundle from a plist.
- `resolveAppPath()` extracts the `.app` path from `java.home` (which will be something like `/Applications/Amigos.app/Contents/runtime/Contents/Home` when packaged).
- Returns `null` during development (when running via `./gradlew run`) so it doesn't install a broken plist.

**Step 2: Call `installIfNeeded()` from `Main.kt`**

Add this as the first line in the `main()` function:

```kotlin
fun main() {
    LaunchAgentInstaller.installIfNeeded()

    application {
    // ... rest unchanged
```

**Step 3: Verify it builds**

Run: `./gradlew build`
Expected: BUILD SUCCESSFUL

**Step 4: Commit**

```bash
git add src/main/kotlin/com/efetepe/amigos/LaunchAgentInstaller.kt src/main/kotlin/com/efetepe/amigos/Main.kt
git commit -m "feat: install LaunchAgent on first run for auto-launch on login"
```

---

### Task 4: Build the DMG and verify

**Step 1: Build the DMG**

```bash
./gradlew packageDmg
```

Expected: BUILD SUCCESSFUL. Output at `build/compose/binaries/main/dmg/Amigos-1.0.0.dmg`

**Step 2: Manual verification checklist**

1. Open the `.dmg` — should show the HAL 9000 icon on the Amigos app
2. Drag to `/Applications`
3. Right-click > Open (first time, to bypass Gatekeeper)
4. Verify: tray icon appears in menu bar
5. Verify: app does NOT appear in Dock or Cmd+Tab
6. Verify: `~/Library/LaunchAgents/com.efetepe.amigos.plist` was created
7. Verify: clicking tray icon opens the main window
8. Quit and relaunch — confirm the plist install is idempotent (no errors)

**Step 3: Commit any final adjustments**

```bash
git add -A
git commit -m "chore: finalize macOS app packaging"
```
