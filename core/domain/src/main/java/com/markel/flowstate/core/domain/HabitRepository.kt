package com.markel.flowstate.core.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {
    fun getHabits(): Flow<List<Habit>>
    fun getEntriesForHabit(habitId: Int): Flow<List<LocalDate>>
    suspend fun insertHabit(habit: Habit)
    suspend fun deleteHabit(habit: Habit)
    suspend fun toggleEntry(habitId: Int, date: LocalDate)
    fun getAllEntries(): Flow<List<HabitEntryFlat>>
}