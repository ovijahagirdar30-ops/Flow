package com.markel.flowstate.core.data.di

import com.markel.flowstate.core.data.ai.GeminiEncouragementGenerator
import com.markel.flowstate.core.domain.EncouragementGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Binds the [EncouragementGenerator] seam. GeminiEncouragementGenerator
 * (plain REST) writes the night check-in's closing line and silently falls
 * back to LocalEncouragementGenerator whenever the key is missing or the
 * call fails — same pattern as PlannerModule binds the evening planner.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class EncouragementModule {

    @Binds
    @Singleton
    abstract fun bindEncouragementGenerator(impl: GeminiEncouragementGenerator): EncouragementGenerator
}
