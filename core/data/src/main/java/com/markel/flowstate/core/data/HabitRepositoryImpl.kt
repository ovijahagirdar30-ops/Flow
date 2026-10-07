package com.markel.flowstate.core.data

import com.markel.flowstate.core.data.local.HabitDao
import com.markel.flowstate.core.data.local.HabitEntity
import com.markel.flowstate.core.data.local.HabitEntryEntity
import com.markel.flowstate.core.data.local.HabitNumericEntryEntity
import com.markel.flowstate.core.data.local.HabitWithEntries
import com.markel.flowstate.core.domain.Habit
import com.markel.flowstate.core.domain.HabitEntryFlat
import com.markel.flowstate.core.domain.HabitNumericEntry
import com.markel.flowstate.core.domain.HabitRepository
import com.markel.flowstate.core.domain.HabitSchedule
import com.markel.flowstate.core.domain.HabitType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
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

    override suspend fun getHabitById(id: Int): Habit? =
        dao.getHabitById(id)?.toDomain()

    override fun getHabitFlow(id: Int): Flow<Habit?> =
        dao.getHabitFlow(id).map { it?.toDomain() }

    override suspend fun insertHabit(habit: Habit) =
        dao.insertHabit(habit.toEntity()).let { Unit }

    override suspend fun updateHabit(habit: Habit) =
        dao.updateHabit(habit.toEntity())

    override suspend fun deleteHabit(habit: Habit) =
        dao.deleteHabit(habit.toEntity())

    override suspend fun toggleEntry(habitId: Int, date: LocalDate, mood: Int?) =
        dao.toggleEntry(habitId, date.toEpochDay(), mood)

    override suspend fun setEntryMood(habitId: Int, date: LocalDate, mood: Int) {
        // Both day tables are written: whichever holds the day's row (boolean
        // → habit_entries, numeric → habit_numeric_entries) takes the rating,
        // the other UPDATE matches nothing — no habit-type lookup needed.
        val epochDay = date.toEpochDay()
        dao.setMood(habitId, epochDay, mood)
        dao.setNumericMood(habitId, epochDay, mood)
    }

    override fun getMoodsForHabit(habitId: Int): Flow<List<HabitEntryFlat>> =
        combine(dao.getMoodsForHabit(habitId), dao.getNumericMoodsForHabit(habitId)) { boolean, numeric ->
            (boolean + numeric)
                .map { HabitEntryFlat(it.habitId, it.epochDay, it.mood) }
                .sortedByDescending { it.epochDay }
        }

    override fun getAllEntries(): Flow<List<HabitEntryFlat>> =  // boolean habits only
        dao.getAllEntries().map { list ->
            list.map { HabitEntryFlat(it.habitId, it.epochDay, it.mood) }
        }

    override fun getAllNumericEntries(): Flow<List<HabitNumericEntry>> =
        dao.getAllNumericEntries().map { list ->
            list.map { it.toDomain() }
        }

    override fun getNumericEntries(habitId: Int): Flow<List<HabitNumericEntry>> =
        dao.getNumericEntries(habitId).map { entries ->
            entries.map { it.toDomain() }
        }

    override suspend fun logNumericEntry(habitId: Int, date: LocalDate, value: Float) {
        // upsertNumericEntry REPLACEs the whole row, so today's rating would
        // be wiped by every increment/edit — carry it across.
        val epochDay = date.toEpochDay()
        val existing = dao.getNumericEntryOnce(habitId, epochDay)
        dao.upsertNumericEntry(
            HabitNumericEntryEntity(
                habitId = habitId,
                epochDay = epochDay,
                value = value,
                mood = existing?.mood
            )
        )
    }

    override suspend fun deleteNumericEntry(habitId: Int, date: LocalDate) =
        dao.deleteNumericEntry(habitId, date.toEpochDay())

    override suspend fun updatePositions(positions: List<Pair<Int, Int>>) {
        positions.forEach { (id, position) -> dao.updatePosition(id, position) }
    }

    override suspend fun updatePriorityRanks(ranks: List<Pair<Int, Int>>) {
        ranks.forEach { (id, rank) -> dao.updatePriorityRank(id, rank) }
    }

    // --- Mappers ---

    private fun HabitEntity.toDomain() = Habit(
        id = id,
        name = name,
        iconName = iconName,
        colorArgb = colorArgb,
        schedule = HabitSchedule.decode(frequency),
        createdAt = LocalDate.ofEpochDay(createdAt / 86400000),
        habitType = HabitType.valueOf(habitType),
        unit = unit,
        targetValue = targetValue,
        step = step,
        position = position,
        priorityRank = priorityRank,
        rolloverIfMissed = rolloverIfMissed,
        moodLoggingEnabled = moodLoggingEnabled,
        reminderEnabled = reminderEnabled,
        reminderMinuteOfDay = reminderMinuteOfDay
    )

    private fun Habit.toEntity() = HabitEntity(
        id = id,
        name = name,
        iconName = iconName,
        colorArgb = colorArgb,
        frequency = schedule.encode(),
        createdAt = createdAt.toEpochDay() * 86400000,
        habitType = habitType.name,
        unit = unit,
        targetValue = targetValue,
        step = step,
        position = position,
        priorityRank = priorityRank,
        rolloverIfMissed = rolloverIfMissed,
        moodLoggingEnabled = moodLoggingEnabled,
        reminderEnabled = reminderEnabled,
        reminderMinuteOfDay = reminderMinuteOfDay
    )

    private fun HabitNumericEntryEntity.toDomain() = HabitNumericEntry(
        habitId = habitId,
        date = LocalDate.ofEpochDay(epochDay),
        value = value
    )
}