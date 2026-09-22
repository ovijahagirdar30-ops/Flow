package com.markel.flowstate.core.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckinDao {
    @Upsert
    suspend fun upsertCheckin(checkin: CheckinEntity)

    @Query("SELECT * FROM checkins WHERE date = :date")
    suspend fun getCheckinByDate(date: String): CheckinEntity?

    @Query("SELECT * FROM checkins ORDER BY date DESC")
    fun getAllCheckins(): Flow<List<CheckinEntity>>
}