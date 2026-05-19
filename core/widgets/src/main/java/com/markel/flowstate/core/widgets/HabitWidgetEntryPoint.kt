package com.markel.flowstate.core.widgets

import com.markel.flowstate.core.domain.HabitRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Bridge between Hilt and Glance classes that do NOT support @AndroidEntryPoint
 * (GlanceAppWidget, ActionCallback).
 *
 * Exposes the HabitRepository from the domain layer instead of the HabitDao from data,
 * to respect Clean Architecture and avoid the need to add methods to the DAO.
 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface HabitWidgetEntryPoint {
    fun habitRepository(): HabitRepository
}
