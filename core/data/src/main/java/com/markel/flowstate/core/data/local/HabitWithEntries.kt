package com.markel.flowstate.core.data.local

import androidx.room.Embedded
import androidx.room.Relation

data class HabitWithEntries(
    @Embedded val habit: HabitEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "habitId"
    )
    val entries: List<HabitEntryEntity>
)