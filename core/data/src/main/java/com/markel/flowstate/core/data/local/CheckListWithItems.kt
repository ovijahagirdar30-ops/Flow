package com.markel.flowstate.core.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class CheckListWithItems(
    @Embedded val list: CheckListEntity,

    @Relation(
        parentColumn = "id",    // ID in CheckListEntity
        entityColumn = "listId" // ID in CheckListItemEntity
    )
    val items: List<CheckListItemEntity>
)