package com.markel.flowstate.core.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "habits")
data class HabitEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val iconName: String = "self_improvement",
    val colorArgb: Int = 0xFF6650A4.toInt(),
    val frequency: String = "DAILY",
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "habit_entries",
    foreignKeys = [
        androidx.room.ForeignKey(
            entity = HabitEntity::class,
            parentColumns = ["id"],
            childColumns = ["habitId"],
            onDelete = androidx.room.ForeignKey.CASCADE
        )
    ],
    indices = [
        androidx.room.Index(value = ["habitId"])
    ]
)
data class HabitEntryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val habitId: Int,
    val completedAt: Long
)

data class HabitEntryFlatEntity(val habitId: Int, val epochDay: Long)