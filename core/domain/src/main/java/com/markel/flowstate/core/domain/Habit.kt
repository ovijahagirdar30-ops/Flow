package com.markel.flowstate.core.domain

import java.time.LocalDate

enum class HabitType { BOOLEAN, NUMERIC }

data class Habit(
    val id: Int = 0,
    val name: String,
    val iconName: String = "self_improvement",
    val colorArgb: Int = 0xFF6650A4.toInt(),
    val schedule: HabitSchedule = HabitSchedule.DAILY,
    val createdAt: LocalDate = LocalDate.now(),
    val habitType: HabitType = HabitType.BOOLEAN,
    val unit: String? = null,
    val targetValue: Float? = null,
    val step: Float = 1f,
    val position: Int = 0,
    val priorityRank: Int = 5, // position in the priority list: 1 = top = most important, kept as a unique 1..N — what gets cut first when the evening plan doesn't have room for everything
    val rolloverIfMissed: Boolean = false, // if true, a missed day carries into tomorrow's plan instead of just being skipped
    /**
     * Opt-in mood logging: when true, completing this habit asks "how did it
     * feel?" and the 1-5 rating lands in the Mood tab plus the last-7-day
     * history the AI planner reads. Off by default — asking every habit every
     * day would defeat the "reduce decisions" goal.
     */
    val moodLoggingEnabled: Boolean = false
)

data class HabitWithStatus(
    val habit: Habit,
    val isCompletedToday: Boolean,
    val streak: Int = 0,
    val todayValue: Float? = null,
    /**
     * False when the habit's schedule says it isn't on today (off-day, or a
     * times-per-week target already met this week) — lists, the header
     * progress and the evening plan all skip it.
     */
    val isDueToday: Boolean = true,
)

data class HabitEntryFlat(val habitId: Int, val epochDay: Long, val mood: Int? = null)

data class HabitNumericEntry(val habitId: Int, val date: LocalDate, val value: Float)