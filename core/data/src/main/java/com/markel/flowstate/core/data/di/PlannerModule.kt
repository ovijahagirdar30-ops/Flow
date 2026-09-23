package com.markel.flowstate.core.data.di

import com.markel.flowstate.core.data.ai.GeminiEveningPlanner
import com.markel.flowstate.core.domain.EveningPlanner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the [EveningPlanner] seam. Stage 3: GeminiEveningPlanner (REST to
 * the Gemini Developer API, falling back to LocalEveningPlanner whenever the
 * key is missing or the call fails). Swapping backends later — the Gen AI
 * Kotlin SDK, a Python service, Ollama — is again just this one @Binds line.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlannerModule {

    @Binds
    abstract fun bindEveningPlanner(impl: GeminiEveningPlanner): EveningPlanner
}
