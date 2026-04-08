package com.markel.flowstate.core.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface IdeaDao {
    @Query("SELECT * FROM ideas ORDER BY position ASC, createdAt DESC")
    fun getIdeas(): Flow<List<IdeaEntity>>

    @Upsert
    suspend fun upsertIdea(idea: IdeaEntity): Long

    @Delete
    suspend fun deleteIdea(idea: IdeaEntity)

    @Query("SELECT * FROM ideas WHERE id = :id")
    suspend fun getIdeaById(id: Int): IdeaEntity?

    @Update
    suspend fun updateIdeas(ideas: List<IdeaEntity>)
}