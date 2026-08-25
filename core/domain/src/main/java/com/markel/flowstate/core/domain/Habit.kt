package com.markel.flowstate.core.domain

import java.time.LocalDate

enum class HabitFrequency { DAILY, WEEKLY }
enum class HabitType { BOOLEAN, NUMERIC }

data class Habit(
    val id: Int = 0,
    val name: String,
    val iconName: String = "self_improvement",
    val colorArgb: Int = 0xFF6650A4.toInt(),
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val createdAt: LocalDate = LocalDate.now(),
    val habitType: HabitType = HabitType.BOOLEAN,
    val unit: String? = null,
    val targetValue: Float? = null,
    val step: Float = 1f,
    val position: Int = 0,
    val priorityRank: Int = 5, // 1 (lowest) – 10 (highest) — what gets cut first when the evening plan doesn't have room for everything
    val rolloverIfMissed: Boolean = false // if true, a missed day carries into tomorrow's plan instead of just being skipped
)

data class HabitWithStatus(
    val habit: Habit,
    val isCompletedToday: Boolean,
    val streak: Int = 0,
    val todayValue: Float? = null,
)

data class HabitEntryFlat(val habitId: Int, val epochDay: Long, val mood: Int? = null)

data class HabitNumericEntry(val habitId: Int, val date: LocalDate, val value: Float)