package com.markel.flowstate.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class TaskEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    val description: String = "",
    val isDone: Boolean = false,
    val position: Int = 0,
    val priority: Int = 0,
    val dueDate: Long? = null,
    val completedAt: Long? = null,
    val reminderTime: Long? = null,
    val categoryId: Int? = null
)