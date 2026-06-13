package com.markel.flowstate.core.data.local

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {

    @Query("SELECT * FROM categories ORDER BY position ASC")
    fun getCategories(): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE id = :id")
    suspend fun getCategoryById(id: Int): CategoryEntity?

    @Upsert
    suspend fun upsertCategory(category: CategoryEntity): Long

    @Query("DELETE FROM categories WHERE id = :id")
    suspend fun deleteCategory(id: Int)

    @Update
    suspend fun updateCategories(categories: List<CategoryEntity>)

    // ── One-shot query (for backup) ──────────────────────────────────

    @Query("SELECT * FROM categories ORDER BY position ASC")
    suspend fun getAllCategoriesOnce(): List<CategoryEntity>

    // ── Bulk operations for category deletion ────────────────────────

    @Query("UPDATE tasks SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun moveTasksToGeneral(categoryId: Int)

    @Query("UPDATE ideas SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun moveIdeasToGeneral(categoryId: Int)

    @Query("UPDATE checklists SET categoryId = NULL WHERE categoryId = :categoryId")
    suspend fun moveCheckListsToGeneral(categoryId: Int)

    @Query("DELETE FROM tasks WHERE categoryId = :categoryId")
    suspend fun deleteTasksInCategory(categoryId: Int)

    @Query("DELETE FROM ideas WHERE categoryId = :categoryId")
    suspend fun deleteIdeasInCategory(categoryId: Int)

    @Query("DELETE FROM checklists WHERE categoryId = :categoryId")
    suspend fun deleteCheckListsInCategory(categoryId: Int)
}
