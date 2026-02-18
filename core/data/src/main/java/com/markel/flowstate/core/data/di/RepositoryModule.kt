package com.markel.flowstate.core.data.di

import com.markel.flowstate.core.data.CheckListRepositoryImpl
import com.markel.flowstate.core.data.IdeaRepositoryImpl
import com.markel.flowstate.core.data.TaskRepositoryImpl
import com.markel.flowstate.core.domain.CheckListRepository
import com.markel.flowstate.core.domain.IdeaRepository
import com.markel.flowstate.core.domain.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    // Hilt, when someone asks for a "TaskRepository" (interface),
    // provide an instance of "TaskRepositoryImpl" (class)
    abstract fun bindTaskRepository(
        taskRepositoryImpl: TaskRepositoryImpl
    ): TaskRepository

    @Binds
    @Singleton
    abstract fun bindIdeaRepository(impl: IdeaRepositoryImpl): IdeaRepository

    @Binds
    @Singleton
    abstract fun bindCheckListRepository(impl: CheckListRepositoryImpl): CheckListRepository
}