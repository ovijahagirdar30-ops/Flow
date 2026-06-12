package com.markel.flowstate.core.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CheckListDao {
    @Transaction
    @Query("SELECT * FROM checklists ORDER BY position ASC")
    fun getListsWithItems(): Flow<List<CheckListWithItems>>

    @Upsert
    suspend fun upsertListEntity(list: CheckListEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertListItems(items: List<CheckListItemEntity>)

    @Query("DELETE FROM checklist_items WHERE listId = :listId")
    suspend fun deleteItemsByListId(listId: Int)

    @Delete
    suspend fun deleteListEntity(list: CheckListEntity)

    @Update
    suspend fun updateCheckLists(lists: List<CheckListEntity>)

    @Transaction
    suspend fun upsertFullList(list: CheckListEntity, items: List<CheckListItemEntity>): Int  {
        // We save/update the parent
        val listId = upsertListEntity(list).toInt()

        // Determine real ID
        val finalId = if (list.id == 0) listId else list.id

        // Delete all subitems to avoid duplicates or ghost items
        deleteItemsByListId(finalId)

        // Insert new subitems
        val itemsWithId = items.map { it.copy(listId = finalId) }
        insertListItems(itemsWithId)

        return finalId
    }

    // ── One-shot query (for backup) ──────────────────────────────────

    @Transaction
    @Query("SELECT * FROM checklists ORDER BY position ASC")
    suspend fun getAllListsOnce(): List<CheckListWithItems>
}