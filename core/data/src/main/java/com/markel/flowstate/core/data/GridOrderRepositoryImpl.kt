package com.markel.flowstate.core.data

import com.markel.flowstate.core.data.local.GridOrderDao
import com.markel.flowstate.core.data.local.GridOrderEntity
import com.markel.flowstate.core.domain.GridOrderEntry
import com.markel.flowstate.core.domain.GridOrderRepository
import com.markel.flowstate.core.domain.GridItemType
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class GridOrderRepositoryImpl @Inject constructor(
    private val dao: GridOrderDao
) : GridOrderRepository {

    override fun getOrder(): Flow<List<GridOrderEntry>> =
        dao.getAll().map { entities -> entities.map { it.toDomain() } }

    override suspend fun saveOrder(entries: List<GridOrderEntry>) {
        dao.replaceAll(entries.map { it.toEntity() })
    }

    override suspend fun addEntry(itemId: Int, itemType: GridItemType) {
        dao.upsert(GridOrderEntity(itemId = itemId, itemType = itemType.name, position = Int.MIN_VALUE))
    }

    override suspend fun removeEntry(itemId: Int, itemType: GridItemType) {
        dao.delete(itemId, itemType.name)
    }

    // Mappers
    private fun GridOrderEntity.toDomain() = GridOrderEntry(
        itemId = itemId,
        itemType = GridItemType.valueOf(itemType),
        position = position
    )

    private fun GridOrderEntry.toEntity() = GridOrderEntity(
        itemId = itemId,
        itemType = itemType.name,
        position = position
    )
}