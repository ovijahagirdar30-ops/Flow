package com.markel.flowstate.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "checklists")
data class CheckListEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val color: Long = 0L
)