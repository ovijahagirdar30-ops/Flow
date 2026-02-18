package com.markel.flowstate.core.domain

import kotlinx.coroutines.flow.Flow

interface CheckListRepository {
    fun getLists(): Flow<List<CheckList>>
    suspend fun upsertList(list: CheckList)
    suspend fun deleteList(list: CheckList)
}