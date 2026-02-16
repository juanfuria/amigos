# macOS App Packaging Design

**Goal:** Package Amigos as a proper macOS `.app` inside a `.dmg`, with auto-launch on login.

**Distribution:** Personal use only — no code signing or notarization needed.

---

## 1. App Icon

Use the provided `HAL 9000.icns` file. Move it into the build resources at a stable path (`src/main/packaging/icon.icns`) and reference it from the Gradle config.

## 2. Gradle Packaging Config

Extend the existing `compose.desktop.application.nativeDistributions` block:

- **Icon:** `macOS.iconFile.set(project.file("src/main/packaging/icon.icns"))`
- **Dock name:** `macOS.dockName = "Amigos"`
- **Tray-only mode:** Set `LSUIElement = true` via `macOS.infoPlist.extraKeysRawXml` so the app hides from the Dock and Cmd+Tab, living only in the menu bar tray.
- **Target format:** Keep `TargetFormat.Dmg`.

Build with `./gradlew packageDmg`. Output goes to `build/compose/binaries/main/dmg/`.

## 3. Launch at Login (LaunchAgent)

On first run, the app installs a LaunchAgent plist at `~/Library/LaunchAgents/com.efetepe.amigos.plist`. This plist tells macOS to launch the app when the user logs in.

The plist contains:
- `Label`: `com.efetepe.amigos`
- `ProgramArguments`: path to the app executable inside `/Applications/Amigos.app`
- `RunAtLoad`: true
- `KeepAlive`: false (the app manages its own lifecycle)

The app checks whether the plist already exists before writing, making the operation idempotent. The app resolves its own install path at runtime to write the correct executable path into the plist.

## Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Distribution scope | Personal | No signing/notarization overhead |
| Packaging tool | Compose `packageDmg` | Already configured, uses jpackage |
| App icon source | User-provided `.icns` | High-quality, all sizes included |
| Dock visibility | Tray-only (LSUIElement) | Daemon-style app, stays out of the way |
| Auto-launch | LaunchAgent plist | Standard macOS mechanism, user-scoped |
