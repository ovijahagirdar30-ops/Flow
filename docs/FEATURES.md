# FlowState — Complete Feature Reference

**What it is:** FlowState is a free, ad-free, offline-first Android app for personal productivity and daily wellbeing. It combines a task manager, idea/checklist notebook, habit tracker, calendar, and a uniquely *proactive* evening routine: an AI-assisted check-in that meets you when you get home and hands you a finished plan for the rest of the night.

Everything lives on your phone. There are no accounts, no servers, no analytics and no data collection. The only optional network call in the entire app is the AI evening planner, and it degrades silently to a fully offline planner when unavailable.

---

## At a glance

| Area | What you get |
|---|---|
| **Flow tab** | Tasks (priority, due dates, reminders, subtasks), Ideas (coloured notes), Checklists — in one sectioned list or grid, organized by categories |
| **Calendar tab** | Expandable month view with per-day task dots, tap a day to see/complete its tasks |
| **Habits tab** | Boolean & numeric habits, schedules, streaks, weekly targets, priority ranking, mood logging, per-habit reminders |
| **Plan tab** | Tonight's agreed evening plan as a tickable checklist |
| **Evening check-in** | Greeting → 5 mood questions → recap → AI-generated evening plan, triggered by arriving home (geofence), weekend mornings, or manually |
| **Night review** | 9 PM daily recap of what got done, what's pending, what rolls to tomorrow |
| **Settings** | Appearance/theme, categories, bottom-nav configuration, JSON backup/restore, notifications, about |
| **Home widget** | Per-habit pill widget, tap to complete, live-updating |

---

## 1. Architecture & tech stack

**Approach:** Clean Architecture + MVVM, 100% Jetpack Compose UI, Kotlin, single-activity (plus a separate check-in activity).

**Tech:** Jetpack Compose with Material 3 (incl. Material 3 Expressive APIs), Hilt for DI, Room (database schema version **31**, with full migration history), Jetpack DataStore for preferences, Kotlin Coroutines/Flow, Navigation 3 with per-tab back stacks and shared-element transitions, Glance for home-screen widgets, AlarmManager exact alarms, Play Services Geofencing.

**Module layout (12 Gradle modules):**

```
app                      — MainActivity, navigation host, theme wiring
feature:flow             — tasks, ideas, checklists, categories UI
feature:calendar         — month calendar + daily task list
feature:habits           — habit list, add/edit sheet, habit detail & stats
feature:checkin          — evening check-in flow, plan screen, night review
feature:settings         — settings, appearance, categories, backup, about
core:domain              — clean models, repository interfaces, use cases
core:data                — Room entities/DAOs, repository impls, DataStore,
                           backup export/import, AI planner
core:designsystem        — theme (FlowStateTheme), icons, shared UI pieces
core:notifications       — task/habit reminder scheduling, check-in alarms,
                           geofence manager, boot receiver
core:widgets             — Glance habit widget + config activity
core:testing             — shared test fixtures
```

**Design principles visible throughout the code:**

- **Reduce decisions** — the app gives one concrete answer (one plan, one next step), never menus of options.
- **Opt-in only** — mood logging, habit reminders, categories and widgets are all off until you ask for them. No prompt nobody asked for.
- **Degrade, never break** — every AI/network path has a deterministic offline fallback; the UI never crashes on a failed request.
- **Local-first privacy** — user data never leaves the device except the anonymised AI prompt you explicitly trigger.

---

## 2. Navigation shell

Five bottom-nav tabs — **Flow · Calendar · Habits · Plan · Settings** — each with its **own back stack**. Switching tabs preserves scroll position and transient state; pressing back pops within the tab instead of ejecting you to another one.

- Editors open as full-screen destinations with animations tailored to them (vertical slide for editors, shared-bounds morph for idea/checklist cards).
- The bottom bar auto-hides on full-screen editors.
- Tab order and visibility are user-configurable (see §7.4).

---

## 3. Flow tab — tasks, ideas, checklists

The main workspace: a sectioned list (or grid) of everything you're working on, sliced by category tabs.

### 3.1 Tasks
- **Title + description**, done/undone toggle with completion timestamps.
- **Priority**: High / Medium / Low / None — colour-coded with flag icons.
- **Due dates** with overdue highlighting and date formatting.
- **Reminders**: per-task (and per-subtask) reminder time → exact-alarm scheduled notification on the `task_reminders` channel, with a **"complete" action** directly in the notification. Completing the task cancels its pending alarm.
- **Subtasks**: each with its own title, description, priority, due date, reminder and completion state — reorderable inside the editor.
- **Categories**: assign any task/idea/checklist to a category; category tabs filter the Flow view; the last-visited category is restored on next launch.
- **Swipe to delete** with a **3.5-second undo window** (undo FAB + per-item animation versioning so an undone swipe can't re-trigger).
- **Drag & drop reordering** persisted on drag end, optimistic in UI.
- **List ⇄ grid view** toggle; collapsible dynamic header on scroll.
- **Reminder permission banner** appears in Flow when notification/alarm permission is missing and re-schedules reminders once granted.
- **Empty state** for fresh installs.

### 3.2 Ideas
- Free-form **title + content** capture with a **colour picker** (idea cards are tinted).
- Grid cards, category assignment, drag reorder, full-screen editor with shared-element open animation.

### 3.3 Checklists
- Titled lists with checkable items (add, tick, reorder, delete), colour, category.
- Grid cards + full-screen editor, same shared-element transition.

### 3.4 Categories (cross-cutting)
- Create, rename (including renaming the virtual **"General"** category), drag-reorder, delete — with an option to **permanently delete the items** inside a deleted category.
- A master **enable/disable switch**: turn categories off and everything falls back to General.
- "General" is a fixed row that can never be deleted.

---

## 4. Calendar tab

- **Expandable month grid** — collapses/expands with scroll, weekday header, month picker, "today" indicator.
- **Per-day task dots** — days show task markers derived from due dates and completion dates.
- **Tap a day** → daily task section below with **interactive rows**: tick tasks complete straight from the calendar (cancelling their reminders as you do).
- Month banner identifying the selected month; the day's tasks keep their Flow-tab ordering (manual `position` order from the database).
- Tasks are grouped by due date (or completion date for finished items); unscheduled tasks don't clutter the calendar.

---

## 5. Habits tab

A full habit tracker with two habit types, schedules, streaks, moods and reminders.

### 5.1 Habit types
- **Boolean habits** — done/not-done tap (animated morphing check button).
- **Numeric habits** — measured with a unit (`litres`, `pages`, …), an optional **target value** and an adjustable **step**; log via a numeric input sheet or quick ± increment/decrement on the card. Goal-met days count as "completed".

### 5.2 Scheduling
- Choose **which weekdays** a habit is due (`MO,WE,FR`) and/or a **weekly target** ("any 3 days a week").
- Off-days are skipped — never counted as misses. Weekly targets track *ISO weeks (Mon–Sun)*.
- Off-schedule habits are excluded from today's list, header progress and the evening plan.

### 5.3 Streaks
- One shared streak implementation used everywhere, so list, detail and check-in numbers always agree.
- Unit follows the schedule: **consecutive scheduled days** for plain habits, **consecutive weeks hitting the target** for weekly-target habits (the in-progress week can't break the streak).
- Current + best-ever streaks, plus per-week completion counts.

### 5.4 Priority ranking
- Habits carry a unique **1..N priority rank** (1 = matters most).
- A dedicated **Reorder Priority** mode with drag handles *and* typed rank entry.
- The rank is what the evening planner uses to decide which habits deserve a slot tonight, and which get cut first when time is short.

### 5.5 Mood logging (opt-in)
- Per-habit **"how did it feel?"** switch (available for both boolean and numeric habits).
- When enabled, completing the habit — or crossing a numeric target for the first time that day — opens an **emoji mood prompt** (1–5).
- Ratings land in the habit's **Mood history** card + average, and in the last-7-days data the AI planner is allowed to see (the opt-in flag is treated as consent — switched-off habits contribute nothing).

### 5.6 Reminders (opt-in)
- Per-habit switch + time-of-day picker on the `habit_reminders` channel.
- Fires **only on scheduled days**, re-armed after reboot and on app resume.

### 5.7 Habit detail screen
- **Boolean stats**: current/best streak, last-8-weeks completion bars (8- or 16-week mode), day-of-week completion radar, mood history + average, month calendar.
- **Numeric stats**: daily value evolution, monthly goal card (current vs target, days completed, average, deficit), 18-week heatmap, value distribution, day-of-week averages.
- **Calendar browsing**: 1 month / 3 months / 1 year views with completion percentages.
- Edit/delete the habit, plus a daily-rotating motivational line on the habit list header (7-message cycle keyed to the day of year).

### 5.8 Rollover
- **`rolloverIfMissed`** — a missed day carries into tomorrow's plan instead of being silently skipped (the planner is told it may skip these freely).

---

## 6. Evening check-in, AI planner & night review

The heart of the app — the "come home, get a plan" loop.

### 6.1 Triggers
| Trigger | When |
|---|---|
| **Home geofence** | Arriving home (ENTER only accepted after a *real, recent EXIT* — filters phantom GPS enters) |
| **Weekend morning alarm** | Sat/Sun 08:00 — you're home all day so the geofence never fires |
| **Manual** | "Start check-in" in the Plan tab's FAB menu |
| **Once-per-day debounce** | The arrival check-in never fires twice in one day |

Presentation is OEM-proof: a full-screen-intent high-priority notification on `checkin_channel`, or a direct activity start when overlay permission allows.

### 6.2 The check-in flow
1. **Greeting**
2. **Five mood questions** — energy, sleepiness, stress, headache, motivation (0–10 sliders), each with an optional **free-text comment**
3. **Recap** — your five answers at a glance, each tappable to jump back and amend, closed by the design's **"Day ends at"** row (divider + lavender value) whose time picker sets the end-of-day cutoff the Plan tab's expiry uses
4. **Tasks for tonight** — the design's hand-off step: an add-a-task row (lands in the real tasks table the Flow tab reads), tonight's tasks with a ✕ to remove (alarms cancelled first), today's due **habits read-only**, and a "Show my plan" pill. The planner starts at the recap's "Generate my evening" so it runs while you pick
5. **Plan** — the planner runs (a real loading screen), then the evening timeline appears with **Agree**, **Regenerate** (attach a typed note → the model revises *that* plan) and **Not tonight** (discard)

Fixed header with five progress segments, bottom pill button, back-gesture navigation between steps — the designed chrome runs greeting → recap; the tasks step and the plan step each own their own chrome (heading + own bottom bar), exactly like the design.

### 6.3 The AI evening planner
Builds a **CheckinSnapshot** (mood + comments, incomplete tasks *with pending subtasks*, all habits with status/streak/priority/mood, wall-clock start time — plus any unexpected plans) and asks a model for tonight's plan.

- **Backend:** OpenRouter (OpenAI-compatible REST) with a free-tier model by default; overridable via `local.properties`. Swappable behind the `EveningPlanner` interface — a one-class change.
- **No API key? Wrong key? Network down? Malformed response?** → instant, silent fallback to the deterministic **offline planner**, so the flow always completes.
- **Hardening**: one automatic retry for transient failures (DNS blips, garbage bodies), 5-minute read timeout (reasoning models are slow), JSON fence-stripping, descriptive parse errors, everything logged.
- **Durable memory**: the last 5 correction notes you typed on earlier evenings ride along in every prompt — so "skincare is only 5 minutes" stays true forever.
- **Revise, don't re-roll**: regenerate with a note sends the previous plan + your instruction back to the model.

**Output:** a headline (≤8 words, never guilt-trippy) plus time-ordered **blocks** — `TASK`, `HABIT`, `MEAL`, `REST`, `REFLECTION`, `OTHER` — each with a start time, duration and a short *reason*. The system prompt encodes the product philosophy: decompress first when you're drained, size rest from your actual mood scores, allot subtasks when a task is too big for tonight, one reflection block near 23:00, run to ~23:35, never shame undone work, never invent ids.

### 6.4 Plan tab
- **"Your evening" page** (design frame B): the header — 40px light title + long date + an **Edit/Done** text button — the plan's headline, an "**N of M done**" line over a hairline progress bar, then the timeline; "**+ Add something**" sits under it.
- **Timeline rows**: a 62dp time column (muted, accent on the current block, with a **Now** marker under it), a 22dp ring checkbox that fills accent when ticked, a connector line running down to the next row, and title / "Habit · 30 min" meta / reason note — every colour **dims once ticked** (300ms transitions).
- The most recent **agreed** plan as a checklist, rendered on every resume from Room.
- **Ticking a TASK block marks the real task done** (and cancels its reminder); habit/free blocks tick visually; per-block edits are remapped when re-agreed.
- **Due-gated ticking**: a checkbox only accepts a tick after its block's scheduled start + duration has elapsed — tapping early shows a small disappearing overlay ("Not due yet — you can tick it after 7:54 pm") instead of ticking. Unticking is always allowed.
- **Edit mode** (the Edit/Done pill): reveals the "**Hold and drag to move.**" hint and a drag handle per row. **Dragging a row swaps it between time slots** — start times stay sorted, ticks follow their block, and the handle tap (or a row tap in edit mode) opens the block editor. A drag that would double-book a time is rejected with the same overlay warning as the dialog.
- **Direct plan editing**: every block can be **edited, removed or added** via a block editor dialog (time, duration, title, reason). Edits persist immediately, existing ticks are remapped so nothing gets lost, and each change is recorded as a durable note that rides along in future AI prompts (drags deliberately skip the note — one gesture would write one per crossing).
- **Double-booking guard**: saving an edit (or a new block, or a drag) onto a time already covered by another block is rejected — in the dialog with a small self-hiding warning ("Another task is scheduled during that time"), for drags with the page overlay — so the list/dialog stays as it was. Only a start/duration change you made can conflict; re-saving a block at its own time is always accepted, even if the generated plan already overlaps. Applies identically in the check-in's plan step.
- **Expiry**: after your end-of-day cutoff (or midnight) the plan ages out — yesterday's plan never lingers.
- FAB menu: **Start check-in** and **Edit plan** (jumps straight to the plan step with the agreed plan + ticks loaded, no need to re-answer the mood questions).

### 6.5 Night review (9 PM)
- A daily 21:00 alarm (re-armed after each fire, inexact allowed if exact-alarm permission is withheld) opens the **night check-in page** (design port):
  - Eyebrow header — "NIGHT CHECK-IN" + "Fri, Oct 2" date — and a light hero line whose second half is lavender-soft: ✅ **Completed today** zero → "Today you didn't mark any tasks as complete, *which is fine — sometimes a pause is needed.*"; a lead line reads "N items are waiting for tomorrow, ready when you are."
  - ✅ **Completed today** count (with the "No checkmarks today — showing up still counts." note when it's empty)
  - ⏳ **Still open** — pending + pushed-to-tomorrow as a ring list (14px hollow circles) with "due Sept 28" meta
  - Staggered entrance (fade + 12px rise, 900ms ease-out), a bordered bottom bar with the **"Done for today"** pill, and the closing state: content fades up 8px while a soft planning-light haze blooms bottom-left, then "Good night, Ovi." + a Back link.
- Plus a **closing line** generated from the day's stats (AI seam with local fallback, non-blocking — lists render first), shown under the closing copy.

---

## 7. Settings

### 7.1 Notifications
Opens the system notification settings for the app and live-reflects the granted/denied state (icon + supporting text change).

### 7.2 Appearance
- **Theme mode**: System / Light / Dark.
- **Dynamic color** (Material You) — when on, it overrides the selected preset.
- **Pure surfaces** toggle.
- **System font** toggle.
- **App color**: 8 presets (Green, Rose, Lavender, Sky, Peach, Mint, Lilac, Sand) **plus a custom colour wheel** — an HSV ring with radial saturation, tap or drag to pick any seed colour. Every preset and the custom colour generate a full Material 3 light/dark scheme via HSV tone mapping, and the choice flows through the whole app including the check-in flow.

### 7.3 Categories
See §3.4 — enable switch, create, rename, reorder, delete (with item cleanup option).

### 7.4 Bottom navigation
- Drag to **reorder** tabs; **show/hide** hidden tabs (some tabs are marked required).
- Persisted across launches.

### 7.5 Backup (Integrations)
- **Export** all core data to a single pretty-printed **JSON file** via the system file picker.
- **Restore** from that file with an **additive/upsert merge** — existing records with the same primary key are overwritten, everything else preserved.
- Explicit failure reporting: invalid file, schema mismatch, unknown error; progress + success/failure states in the UI.
- Covers tasks, subtasks, ideas, checklists, checklist items, habits, boolean entries, numeric entries and categories.
- **Known limitation (verified in code):** habit *mood ratings*, and the habit `priorityRank` / `rolloverIfMissed` fields, are **not** in the export schema yet — a restore resets them to defaults. Check-in history, evening plans and plan feedback are deliberately transient and also not exported.

### 7.6 About
App name, version, developer, Apache-2.0 license.

---

## 8. Notifications & reminders (system-level)

| Channel | Purpose |
|---|---|
| `task_reminders` | Task & subtask reminders, with an inline **Complete** action |
| `habit_reminders` | Per-habit reminders on scheduled days only |
| `checkin_channel` | High-priority full-screen check-in / night-review hand-off |

- Exact alarms where permitted, degrading gracefully to inexact.
- **Boot receiver** re-arms task + habit reminders and check-in alarms after reboot; app launch re-arms the nightly/weekend chain.
- A **permission banner** in Flow explains what's missing and re-schedules the moment permission is granted.
- Reminder scheduling logic is shared (`buildAlarmItems`) so Flow, Calendar and the editor can never disagree about what's armed.

---

## 9. Home-screen widget

- A **per-habit pill widget** (Glance): habit icon, today's date badge, completion state.
- **Tap anywhere to toggle** today's completion — updates the app data live (reactive Flow collection, preloaded first frame so it never flashes a placeholder); tinting follows the widget's Material/Glance theme.
- **Config activity** lets you choose which habit the widget represents; the widget shows a "?" placeholder until configured or if the habit was deleted.
- Sized responsively from the actual widget box (`SizeMode.Exact`).

---

## 10. Data, privacy & offline behaviour

- **Room database (v31)** with a complete migration chain — features added over time (categories, habit schedules, numeric mood column, etc.) all ship as real migrations, and exported **schemas are versioned** in the repo.
- **DataStore** for preferences: theme, custom colour, bottom-nav config, last category, end-of-day cutoff, general-category name.
- **No network, ever, except** the optional AI planner call you trigger by completing a check-in.
- **Secrets stay local**: the AI key lives in `local.properties` (never committed); home geofence coordinates live in a gitignored `HomeLocationLocal.kt`.
- **Backup file is yours**: plain JSON, no proprietary format, restorable into any future build that understands its schema version.
- Works 100% offline: install, use, and back up without ever signing in.

---

## 11. Localization

String resources are maintained per feature module in **default / `values-en` / `values-es`** (English and Spanish), covering navigation, settings, habits, calendar, flow and notifications.

---

## 12. Quality

- **Unit tests across modules**: task/flow view-models, task editor, ideas, checklists, habit view-models + detail, habit repository (incl. mood storage), streak math, reminder scheduling, habit reminder planner, calendar, plan block edits, plan expiry — 39 test files in total.
- **Export schemas** for every Room version are committed, so migrations are verifiable.
- Features are validated **end-to-end on a physical device** (alarm fires, geofence registers, DB migration applies, prompts appear, notifications render).

---

## 13. Not yet built (honest gaps)

- No cloud sync / multi-device (by design so far — it's a local-first app).
- No recurring *tasks* (repetition lives in Habits instead).
- No per-day task history: tasks are point-in-time, so a day's plan can't be replayed from history.
- End-of-day cutoff is stored and drives plan expiry, but its picker has no dedicated settings screen yet.
- **Unexpected plans** are fully modelled and planner-aware (they're sent to the model and it schedules around them), but the current check-in UI has no entry form for them — that step is legacy in the ViewModel, so the list is normally empty.
- The check-in's "today's unfinished items" list is carried in UI state but not rendered by the current flow design.
- Backup export misses habit mood ratings and the habit `priorityRank` / `rolloverIfMissed` fields (see §7.5).
- Home location is configured in code (`HomeLocationLocal.kt`), not from a settings screen.
- README's "mood tracking in development" line is stale — habit mood logging and the mood-driven check-in are live; a standalone mood diary is not.

---

*Feature inventory generated from the codebase on 2026-10-07 (branch `master`).*
