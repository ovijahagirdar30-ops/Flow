package com.markel.flowstate.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "grid_order")
data class GridOrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val itemId: Int,
    val itemType: String,  // "TASK", "IDEA", "CHECKLIST"
    val position: Int
)