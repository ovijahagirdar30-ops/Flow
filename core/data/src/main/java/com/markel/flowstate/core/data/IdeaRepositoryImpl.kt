package com.markel.flowstate.core.data

import com.markel.flowstate.core.data.local.IdeaDao
import com.markel.flowstate.core.data.local.IdeaEntity
import com.markel.flowstate.core.domain.Idea
import com.markel.flowstate.core.domain.IdeaRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class IdeaRepositoryImpl @Inject constructor(
    private val ideaDao: IdeaDao
) : IdeaRepository {
    override fun getIdeas(): Flow<List<Idea>> =
        ideaDao.getIdeas().map { entities -> entities.map { it.toDomain() } }

    override suspend fun upsertIdea(idea: Idea): Long = ideaDao.upsertIdea(idea.toEntity())

    override suspend fun deleteIdea(idea: Idea) = ideaDao.deleteIdea(idea.toEntity())

    override suspend fun getIdeaById(id: Int): Idea? {
        return ideaDao.getIdeaById(id)?.toDomain()
    }

    // Mappers
    private fun IdeaEntity.toDomain() = Idea(id, title, content, createdAt, color)
    private fun Idea.toEntity() = IdeaEntity(id, title, content, createdAt, color)
}