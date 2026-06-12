# Notifyer

Notifyer is a customizable Android reminder app built with Kotlin.

Originally started as a simple countdown reminder project, Notifyer has grown into a smart reminder system with multiple reminder types, background notifications, and user customization.

## Current Features

### Countdown Timers

* Create countdown reminders in seconds, minutes, or hours
* Run a timer to fire a notification after the chosen duration — even if the app is closed
* Edit and delete timers

### Scheduled Reminders

* Pick a specific date and time (Material date & time pickers)
* Recurring reminders: daily or weekly
* Fires via AlarmManager even when the app is closed, and re-arms itself after a reboot

### App Triggers

* Fire a reminder when you open a chosen app
* Or remind every N opens — usage-based (e.g. every 5th time you open an app)
* Runs a lightweight foreground watcher (needs Usage access, granted in Settings)

### Reminder Management

* Detailed list: each reminder shows its name and details (duration, date/time + repeat, or app + open count) with a per-type icon
* Floating Action Button (FAB) for creating reminders
* Multi-select: long-press to select, then bulk delete
* Automatic refresh when returning to Home
* Empty state with a call to action
* Persistent local storage (SharedPreferences, JSON)

### Notifications

* Background notifications via AlarmManager
* Asks for the notification permission on first launch (Android 13+)
* Each reminder shows its own notification

### UI

* Material 3 interface
* Outlined text fields, dropdown menus, and Material date/time pickers
* Tap a reminder to expand it in place for Run / Edit / Delete
* Multi-language: English and Vietnamese
* Toast feedback and smooth list animations

### Themes

* Light, Dark, or System (follows the device) mode
* Four accent colours: Blue, Green, Purple, Orange
* Choose from Settings → Theme — your choice is saved and applied on the next launch

### Settings

* In-app language switch: System / English / Vietnamese
* Shortcut to the system notification sound & vibration settings
* Export / import reminders as a JSON file (backup without the cloud)
* Clear all reminders
* About: version and source-code link

### Easter Eggs

* Some hidden surprises may exist.
* Spam clicking random things is encouraged at your own risk.

## Planned Features

### Smart Features

* Birthday greetings
* More personalization options

### Advanced Features

* Multiple reminder categories
* Statistics and usage tracking
* Cloud sync (Google sign-in, restore on another device)

A more detailed roadmap lives in [docs/DEVELOPMENT_PLAN.md](docs/DEVELOPMENT_PLAN.md).

## Tech Stack

* Kotlin
* Android SDK
* RecyclerView
* SharedPreferences (JSON storage)
* AlarmManager + BroadcastReceiver
* UsageStatsManager + foreground service (app triggers)
* Material 3 (Material Components)
* Multi-language (vi / en)

## Development Status

Current Version:
v0.8 (In Development)

Notifyer is actively developed and may contain bugs, unexpected behavior, and questionable design decisions made at 2 AM.

## Credits

Created by DyBe

Special thanks to:

* thanhmobiledev
* Android Studio
* Kotlin
* ChatGPT
* Claude
* Random debugging sessions
* Dell Latitude fighting for its life

## License

MIT License (recommended)
