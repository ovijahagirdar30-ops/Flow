package com.markel.flowstate.core.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface HabitDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHabit(habit: HabitEntity): Long

    @Delete
    suspend fun deleteHabit(habit: HabitEntity)

    @Update
    suspend fun updateHabit(habit: HabitEntity)

    @Transaction
    @Query("SELECT * FROM habits")
    fun getHabitsWithEntries(): Flow<List<HabitWithEntries>>

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun getHabitById(id: Int): HabitEntity?

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
    fun getAllEntries(): Flow<List<HabitEntryFlatEntity>>  // only the entries of boolean habits

    @Query("UPDATE habits SET position = :position WHERE id = :id")
    suspend fun updatePosition(id: Int, position: Int)

    @Query("SELECT * FROM habit_numeric_entries WHERE habitId = :habitId ORDER BY epochDay DESC")
    fun getNumericEntries(habitId: Int): Flow<List<HabitNumericEntryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNumericEntry(entry: HabitNumericEntryEntity)

    @Query("DELETE FROM habit_numeric_entries WHERE habitId = :habitId AND epochDay = :epochDay")
    suspend fun deleteNumericEntry(habitId: Int, epochDay: Long)

}