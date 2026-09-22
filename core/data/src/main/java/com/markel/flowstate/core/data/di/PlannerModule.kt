package com.markel.flowstate.core.data.di

import com.markel.flowstate.core.domain.EveningPlanner
import com.markel.flowstate.core.domain.LocalEveningPlanner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the [EveningPlanner] seam. Stage 2: LocalEveningPlanner (offline,
 * deterministic). When the Gemini-backed implementation lands, this one
 * @Binds is the only line that changes — everything else injects the
 * interface.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlannerModule {

    @Binds
    abstract fun bindEveningPlanner(impl: LocalEveningPlanner): EveningPlanner
}
