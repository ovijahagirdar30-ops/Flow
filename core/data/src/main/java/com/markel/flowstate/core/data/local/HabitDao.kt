package com.markel.flowstate.core.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Transaction
    @Query("SELECT * FROM habits")
    fun getHabitsWithEntries(): Flow<List<HabitWithEntries>>

    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId")
    fun getEntriesForHabit(habitId: Int): Flow<List<HabitEntryEntity>>

    @Query("SELECT * FROM habit_entries WHERE habitId = :habitId AND completedAt = :epochDay")
    suspend fun getEntry(habitId: Int, epochDay: Long): HabitEntryEntity?

    @Insert
    suspend fun insertEntry(entry: HabitEntryEntity)

    @Query("DELETE FROM habit_entries WHERE habitId = :habitId AND completedAt = :epochDay")
    suspend fun deleteEntry(habitId: Int, epochDay: Long)

    // toggle en una sola transacción
    @Transaction
    suspend fun toggleEntry(habitId: Int, epochDay: Long) {
        val existing = getEntry(habitId, epochDay)
        if (existing != null) {
            deleteEntry(habitId, epochDay)
        } else {
            insertEntry(HabitEntryEntity(habitId = habitId, completedAt = epochDay))
        }
    }

    @Query("SELECT habitId, completedAt as epochDay FROM habit_entries")
    fun getAllEntries(): Flow<List<HabitEntryFlatEntity>>
}