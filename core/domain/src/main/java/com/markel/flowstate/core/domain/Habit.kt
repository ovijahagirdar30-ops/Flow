package com.markel.flowstate.core.domain

import java.time.LocalDate

enum class HabitFrequency { DAILY, WEEKLY }

data class Habit(
    val id: Int = 0,
    val name: String,
    val iconName: String = "self_improvement",
    val colorArgb: Int = 0xFF6650A4.toInt(),
    val frequency: HabitFrequency = HabitFrequency.DAILY,
    val createdAt: LocalDate = LocalDate.now()
)

data class HabitWithStatus(
    val habit: Habit,
    val isCompletedToday: Boolean,
    val streak: Int = 0
)