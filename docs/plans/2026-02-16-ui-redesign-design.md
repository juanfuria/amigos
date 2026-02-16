# UI Redesign — Tabbed Main Window

## Goal

Replace the disconnected per-screen windows with a single main window using a NavigationRail sidebar. Add a Friends management tab with inline add/edit/delete. Load tray icon from a PNG resource.

## Changes

### Main window
- Single persistent `Window` (~800x600) with Material3 `NavigationRail` on the left
- Three tabs: Friends (default), Log, Settings
- Closing the window hides it to tray (app keeps running, scheduler stays active)
- Tray menu gets "Open Amigos" item to re-show the window

### Friends tab
- List of friend cards showing: name, preferred channel, days since last contact, frequency
- Edit and Delete buttons per card (delete with confirmation dialog)
- "Add Friend" FAB or button
- Add/Edit shows the form inline (replaces list content), with Cancel to go back

### Log tab
- Reuses existing `LogViewContent` (minus the close button, since navigation is via rail)

### Settings tab
- Reuses existing `SettingsViewContent` (minus the close button)

### Nudge popup
- Stays as a separate small window (unchanged)

### Tray icon
- Loaded from `src/main/resources/tray-icon.png` (user will provide)
- Fallback to generated purple circle if file missing
