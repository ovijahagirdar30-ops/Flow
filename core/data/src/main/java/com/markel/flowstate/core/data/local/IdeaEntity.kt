package com.markel.flowstate.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "ideas")
data class IdeaEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val createdAt: Long,
    val color: Long
)