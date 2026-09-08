package com.rounds.test.to_dolist.data.tasks.di

import com.rounds.test.to_dolist.data.tasks.api.FakeTaskApi
import com.rounds.test.to_dolist.data.tasks.api.TaskApi
import com.rounds.test.to_dolist.data.tasks.repository.DefaultTaskRepository
import com.rounds.test.to_dolist.tasks.repository.TaskRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlin.random.Random

/**
 * Where the `:lib` contracts meet their implementations. Features resolve [TaskRepository] and never
 * learn that [DefaultTaskRepository] or [FakeTaskApi] exist.
 *
 * Swapping [FakeTaskApi] for a real service is a one-line change here.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class DataModule {

    @Binds
    @Singleton
    abstract fun bindTaskRepository(impl: DefaultTaskRepository): TaskRepository

    @Binds
    @Singleton
    abstract fun bindTaskApi(impl: FakeTaskApi): TaskApi

    companion object {

        /**
         * The mock source's dice. Provided rather than defaulted in the constructor so a test — or a
         * debug build — can substitute a seeded [Random] without touching [FakeTaskApi].
         */
        @Provides
        @Singleton
        fun provideRandom(): Random = Random.Default
    }
}
