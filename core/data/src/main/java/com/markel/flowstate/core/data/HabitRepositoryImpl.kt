package com.markel.flowstate.core.data

import com.markel.flowstate.core.data.local.HabitDao
import com.markel.flowstate.core.data.local.HabitEntity
import com.markel.flowstate.core.data.local.HabitEntryEntity
import com.markel.flowstate.core.data.local.HabitWithEntries
import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitEntryFlat
import com.markel.flowstate.core.domain.HabitFrequency
import com.markel.flowstate.core.domain.HabitRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import javax.inject.Inject

class HabitRepositoryImpl @Inject constructor(
    private val dao: HabitDao
) : HabitRepository {

    override fun getHabits(): Flow<List<Habit>> =
        dao.getHabitsWithEntries().map { list -> list.map { it.habit.toDomain() } }

    override fun getEntriesForHabit(habitId: Int): Flow<List<LocalDate>> =
        dao.getEntriesForHabit(habitId).map { entries ->
            entries.map { LocalDate.ofEpochDay(it.completedAt) }
        }

    override suspend fun insertHabit(habit: Habit) =
        dao.insertHabit(habit.toEntity()).let { Unit }

    override suspend fun deleteHabit(habit: Habit) =
        dao.deleteHabit(habit.toEntity())

    override suspend fun toggleEntry(habitId: Int, date: LocalDate) =
        dao.toggleEntry(habitId, date.toEpochDay())

    override fun getAllEntries(): Flow<List<HabitEntryFlat>> =
        dao.getAllEntries().map { list ->
            list.map { HabitEntryFlat(it.habitId, it.epochDay) }
        }

    // --- Mappers ---

    private fun HabitEntity.toDomain() = Habit(
        id = id,
        name = name,
        iconName = iconName,
        colorArgb = colorArgb,
        frequency = HabitFrequency.valueOf(frequency),
        createdAt = LocalDate.ofEpochDay(createdAt / 86400000)
    )

    private fun Habit.toEntity() = HabitEntity(
        id = id,
        name = name,
        iconName = iconName,
        colorArgb = colorArgb,
        frequency = frequency.name,
        createdAt = createdAt.toEpochDay() * 86400000
    )
}