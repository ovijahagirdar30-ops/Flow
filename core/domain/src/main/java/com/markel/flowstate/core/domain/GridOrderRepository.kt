package com.markel.flowstate.core.domain

import kotlinx.coroutines.flow.Flow

interface GridOrderRepository {
    fun getOrder(): Flow<List<GridOrderEntry>>
    suspend fun saveOrder(entries: List<GridOrderEntry>)
    suspend fun addEntry(itemId: Int, itemType: GridItemType)
    suspend fun removeEntry(itemId: Int, itemType: GridItemType)
}

enum class GridItemType { TASK, IDEA, CHECKLIST }

data class GridOrderEntry(
    val itemId: Int,
    val itemType: GridItemType,
    val position: Int
)