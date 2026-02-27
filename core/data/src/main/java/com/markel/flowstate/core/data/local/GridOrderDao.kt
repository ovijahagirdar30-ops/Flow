package com.markel.flowstate.core.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface GridOrderDao {

    @Query("SELECT * FROM grid_order ORDER BY position ASC")
    fun getAll(): Flow<List<GridOrderEntity>>

    @Upsert
    suspend fun upsert(entity: GridOrderEntity)

    @Query("DELETE FROM grid_order WHERE itemId = :itemId AND itemType = :itemType")
    suspend fun delete(itemId: Int, itemType: String)

    @Transaction
    suspend fun replaceAll(entities: List<GridOrderEntity>) {
        deleteAll()
        insertAll(entities)
    }

    @Query("DELETE FROM grid_order")
    suspend fun deleteAll()

    @Insert
    suspend fun insertAll(entities: List<GridOrderEntity>)
}