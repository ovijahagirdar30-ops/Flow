package com.markel.flowstate.core.domain

import kotlinx.coroutines.flow.Flow

interface IdeaRepository {
    fun getIdeas(): Flow<List<Idea>>
    suspend fun upsertIdea(idea: Idea): Long
    suspend fun deleteIdea(idea: Idea)
    suspend fun getIdeaById(id: Int): Idea?
}