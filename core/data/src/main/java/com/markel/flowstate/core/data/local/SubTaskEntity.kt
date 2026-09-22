package com.markel.flowstate.core.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subtasks",
    foreignKeys = [
        ForeignKey(
            entity = TaskEntity::class,
            parentColumns = ["id"],
            childColumns = ["taskId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["taskId"])]
)
data class SubTaskEntity(
    @PrimaryKey
    val id: String,
    val taskId: Int,
    val title: String,
    val description: String,
    val isDone: Boolean,
    val priority: Int,
    val dueDate: Long?,
    val position: Int,
    val completedAt: Long? = null,
    val reminderTime: Long? = null
)