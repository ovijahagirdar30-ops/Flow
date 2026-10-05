package com.markel.flowstate.core.domain

import com.markel.flowstate.core.domain.checkin.Checkin

/**
 * Everything the AI brain needs to generate an evening plan, captured at one
 * point in time:
 *
 *  - [checkin] — today's saved check-in: mood values, per-slider comments and
 *    unexpected plans. Null if the check-in hasn't been completed/saved yet
 *    (the AI flow always saves first, so it will be non-null in practice).
 *  - [tasks] — currently-incomplete tasks, i.e. what's left to plan tonight.
 *  - [habits] — every habit with today's completion status and streak, so the
 *    AI can see both what's already done and what remains.
 *  - [habitMoods] — the last 7 days of 1-5 mood ratings per habit, but ONLY
 *    for habits whose mood logging is switched on: that flag is the user's
 *    consent, so a habit turned off contributes nothing here even if it has
 *    older ratings in the database.
 *
 * Built by BuildCheckinSnapshotUseCase. Deliberately a plain immutable data
 * class with NO serialization annotations: how this crosses the process /
 * network boundary (in-app Gemini DTO, Python backend JSON, Ollama prompt)
 * belongs to the backend decision and gets added there — without changing
 * this shape.
 *
 * Evolution notes: re-invoking with an earlier date replays that day's
 * check-in + habit status (tasks are always point-in-time current, since
 * Room has no per-day task history). Adaptive re-planning during the evening
 * just means calling this again with the same date.
 */
data class CheckinSnapshot(
    /** ISO date (yyyy-MM-dd, e.g. "2026-09-22") this snapshot is scoped to. */
    val date: String,
    /** Today's saved check-in (mood + comments + unexpected plans); null if not yet completed. */
    val checkin: Checkin?,
    /** Incomplete tasks as of snapshot time — what's left to plan. */
    val tasks: List<Task>,
    /** All habits with completion status, streaks and today's numeric values. */
    val habits: List<HabitWithStatus>,
    /**
     * Mood logging per habit id, oldest first — only habits that opted in.
     * Empty when nothing has mood logging on (the default), which is exactly
     * what should happen until the user turns it on for a habit.
     */
    val habitMoods: Map<Int, List<HabitMoodLog>> = emptyMap()
)

/** One day's mood rating (1-5) for a single habit. */
data class HabitMoodLog(
    /** ISO date (yyyy-MM-dd) the rating was captured on. */
    val date: String,
    /** The 1-5 emoji rating picked on the mood prompt. */
    val mood: Int
)
