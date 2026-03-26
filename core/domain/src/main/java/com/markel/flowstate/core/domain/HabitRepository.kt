package com.markel.flowstate.core.domain

import kotlinx.coroutines.flow.Flow
import java.time.LocalDate

interface HabitRepository {
    fun getHabits(): Flow<List<Habit>>
    fun getEntriesForHabit(habitId: Int): Flow<List<LocalDate>>
    suspend fun insertHabit(habit: Habit)
    suspend fun updateHabit(habit: Habit)
    suspend fun deleteHabit(habit: Habit)
    suspend fun toggleEntry(habitId: Int, date: LocalDate)
    fun getAllEntries(): Flow<List<HabitEntryFlat>>
    suspend fun getHabitById(id: Int): Habit?
    fun getNumericEntries(habitId: Int): Flow<List<HabitNumericEntry>>
    suspend fun logNumericEntry(habitId: Int, date: LocalDate, value: Float)
    suspend fun deleteNumericEntry(habitId: Int, date: LocalDate)
    suspend fun updatePositions(positions: List<Pair<Int, Int>>)
}