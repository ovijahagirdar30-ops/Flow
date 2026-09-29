package com.markel.flowstate.core.data.di

import com.markel.flowstate.core.data.ai.OpenRouterEveningPlanner
import com.markel.flowstate.core.domain.EveningPlanner
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * Binds the [EveningPlanner] seam. Stage 4: OpenRouterEveningPlanner (REST to
 * OpenRouter's OpenAI-compatible endpoint, falling back to LocalEveningPlanner
 * whenever the key is missing or the call fails). Swapped from
 * GeminiEveningPlanner when the Gemini project was denied generateContent
 * access; swapping back later is again just this one @Binds line.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class PlannerModule {

    @Binds
    abstract fun bindEveningPlanner(impl: OpenRouterEveningPlanner): EveningPlanner
}
