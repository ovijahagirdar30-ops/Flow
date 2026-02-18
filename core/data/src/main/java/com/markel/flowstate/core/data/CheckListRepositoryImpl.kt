package com.markel.flowstate.core.data

import com.markel.flowstate.core.data.local.CheckListDao
import com.markel.flowstate.core.data.local.CheckListEntity
import com.markel.flowstate.core.data.local.CheckListItemEntity
import com.markel.flowstate.core.data.local.CheckListWithItems
import com.markel.flowstate.core.domain.CheckList
import com.markel.flowstate.core.domain.CheckListItem
import com.markel.flowstate.core.domain.CheckListRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject

class CheckListRepositoryImpl @Inject constructor(
    private val checkListDao: CheckListDao
) : CheckListRepository {
    override fun getLists(): Flow<List<CheckList>> =
        checkListDao.getListsWithItems().map { list -> list.map { it.toDomain() } }

    override suspend fun upsertList(list: CheckList) {
        checkListDao.upsertFullList(
            list.toEntity(),
            list.items.map { it.toEntity(list.id) }
        )
    }

    override suspend fun deleteList(list: CheckList) =
        checkListDao.deleteListEntity(list.toEntity())

    // Mappers
    private fun CheckListWithItems.toDomain() = CheckList(
        id = list.id,
        title = list.title,
        items = items.map { CheckListItem(it.id, it.text, it.isDone, it.position) }
    )
    private fun CheckList.toEntity() = CheckListEntity(id, title)
    private fun CheckListItem.toEntity(listId: Int) = CheckListItemEntity(id, listId, text, isDone, position)
}