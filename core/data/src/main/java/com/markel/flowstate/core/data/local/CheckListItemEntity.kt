package com.markel.flowstate.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(
    tableName = "checklist_items",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = CheckListEntity::class,
            parentColumns = ["id"],
            childColumns = ["listId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ]
)
data class CheckListItemEntity(
    @PrimaryKey val id: String,
    val listId: Int,
    val text: String,
    val isDone: Boolean,
    val position: Int
)