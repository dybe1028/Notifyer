# Notifyer - Development Plan

## Project Overview

Notifyer is a customizable Android reminder application built using Kotlin.

The goal of Notifyer is to provide a flexible reminder experience that users can personalize according to their needs.

The project prioritizes:

* Simplicity
* Customizability
* Reliability
* Incremental development

Notifyer is currently an actively developed personal project, not a large-scale commercial application.

---

# Current Technology Stack

* Kotlin
* Android SDK
* RecyclerView
* SharedPreferences (JSON-encoded ordered list)
* AlarmManager (background notifications)
* Material 3
* Android Studio

Current architecture should be preserved unless a change provides significant benefits.

---

# Existing Features

## Reminder List

* Display reminders using RecyclerView.
* Support empty state ("No timer yet").

## Countdown Reminders

* Create countdown timers.
* Display countdown information.
* Run countdown reminders.
* Edit countdown reminders.
* Delete countdown reminders.

## Scheduled Reminders

* Create reminders for a specific date + time (DatePicker / TimePicker).
* Fire via AlarmManager even when the app is closed.
* Edit and delete scheduled reminders.

## Persistence

* Save reminders locally using SharedPreferences.
* Automatically refresh the reminder list when returning to MainActivity.

## UI

* Floating Action Button for adding reminders.
* Simple Material 3 interface.

---

# Implementation Status (updated 2026-06-11)

This section reconciles the plan with what is actually in the codebase after the
critical-bug fix pass. The original plan text above describes the pre-fix baseline;
the notes here record what changed and why. **Structure was kept intact** — still
SharedPreferences, no DB, no large frameworks.

## Done

* **Storage**: SharedPreferences now holds a JSON-encoded ordered list keyed by a
  stable reminder `id` (was a `StringSet`). Reason: `StringSet` dropped duplicate
  entries, reordered on every read, and broke position-based edit/delete.
  SharedPreferences itself was **not** replaced.
* **Background notifications (Phase 2 core)**: AlarmManager + a `BroadcastReceiver`
  now fire reminders when the app is closed; a boot receiver re-arms pending alarms
  after reboot. Falls back to an inexact alarm when the exact-alarm permission is
  not granted.
* **Notification runtime permission**: `POST_NOTIFICATIONS` is requested on first
  launch (Android 13+).
* **Schedule rendering**: timer and schedule reminders render correctly (the old
  adapter mis-parsed schedule rows).
* **Leak fix**: the in-adapter `CountDownTimer` was removed; "Run" now schedules a
  real alarm instead of counting down on the UI thread.
* **i18n**: user-facing strings extracted to `values/strings.xml` + `values-vi/`.
* **Package / version**: `applicationId` + `namespace` = `com.dybe.notifyer`;
  `versionName` = `0.6`; footer version reads from `BuildConfig.VERSION_NAME`.
* **Phase 1 — Multi-Select**: long-press starts an ActionMode; tap toggles rows in
  selection mode; bulk delete (cancels alarms first) with a confirm dialog. Normal
  tap → Run/Edit/Delete popup is unchanged. No new libraries; reuses the existing
  checkbox in `item_notify.xml`.
* **Phase 2 — Timer units + recurring**: countdown timers accept seconds / minutes /
  hours (stored as canonical seconds, displayed in the largest exact unit); schedules
  support daily / weekly repeat, re-armed by the receiver after each fire. No model
  rewrite — added one optional `repeat` enum field (old data defaults to NONE).
* **Phase 5 — UX polish**: empty state with a bell icon + CTA hint; list items show a
  per-type icon on rounded cards with a touch ripple + a load animation; create screens
  use Material 3 outlined text fields (date/time carry leading icons); date/time use
  `MaterialDatePicker` + `MaterialTimePicker` (past dates disabled); dialogs use
  MaterialAlertDialog; the per-item action menu (Run / Edit / Delete) is a custom
  Material popup with icons and a destructive-red Delete, dropped right under the
  tapped row (replaces the plain `PopupMenu`); colours centralised in `colors.xml`;
  remaining create-screen strings extracted. Layout/theme only — create/edit logic unchanged.

## Supporting classes added (small, single-purpose — not a framework)

* `Reminder` (data model), `ReminderRepository` (ordered id-based storage),
  `ReminderScheduler` (AlarmManager), `ReminderReceiver`, `BootReceiver`,
  `NotificationHelper`, `Constants`.

---

# Development Principles

1. Existing features must not break.

2. Countdown functionality is considered critical and must continue working after every feature addition.

3. Avoid unnecessary refactoring.

4. Avoid introducing complex architectures that are not required by the project's scale.

5. New features should integrate naturally with the current UI.

---

# Planned Features

## Phase 1 - Reminder Improvements

### Multi-Select Mode

> Status: **implemented** (ActionMode + bulk delete). Verified on emulator.

Requirements:

* Long press enters selection mode.
* Use ActionMode.
* Multiple reminders can be selected.
* Bulk delete selected reminders.
* Cancel selection mode.

Normal tap behavior must remain unchanged.

---

## Phase 2 - Scheduled Reminders

> Status: **implemented** — date/time reminders + seconds/minutes/hours timer units +
> daily/weekly recurring, all firing via AlarmManager even when the app is closed.

Requirements:

Support reminders based on:

* Seconds
* Minutes
* Hours
* Specific dates
* Specific times

Users should choose reminder type from a menu shown after pressing the add button.

Implementation should eventually use AlarmManager.

---

## Phase 3 - App Trigger Reminders

Requirements:

Allow users to configure reminders that appear when specific applications are opened.

Example:

"When App X is opened, show Reminder Y."

This feature should be designed carefully due to Android permission limitations.

---

## Phase 4 - Usage-Based Reminders

Requirements:

Track app launch counts.

Examples:

* Open App A once → Notification A
* Open App A five times → Notification B

Users must be able to configure these rules.

---

## Phase 5 - User Experience Improvements

> Status: **largely done** — empty state (icon + CTA), per-type item icons + rounded
> cards + ripple, centralised colours, Material 3 outlined create fields,
> MaterialAlertDialog, list load animation. Optional remaining: per-item add/remove
> animations (DiffUtil).

Requirements:

* Improve visual consistency.
* Better empty states.
* Better dialogs.
* Smoother interactions.
* Maintain a simple Material 3 design.

---

## Phase 6 - Cloud Synchronization

Requirements:

Google login support.

Allow users to:

* Save reminders to the cloud.
* Restore reminders on another device.
* Sync application settings.

Implementation details can be proposed later.

---

# Important Constraints

Do NOT:

* Replace SharedPreferences unless necessary.
* Rewrite the entire project architecture.
* Introduce large frameworks without clear justification.
* Break countdown functionality.
* Remove existing features.

Do:

* Make incremental improvements.
* Clearly explain architectural recommendations.
* Identify risks before implementation.
* List modified files after completing each feature.

---

# Success Criteria

Notifyer should evolve into:

"A smart reminder application that remains lightweight, customizable, and easy to use."

The application should feel practical rather than overly complicated.
