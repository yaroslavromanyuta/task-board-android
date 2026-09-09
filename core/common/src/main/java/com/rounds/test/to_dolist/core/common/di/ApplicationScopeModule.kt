package com.rounds.test.to_dolist.core.common.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope

/**
 * A scope that outlives every screen, for the writes that have to.
 *
 * `viewModelScope` is the right owner for work whose only purpose is to update a screen, because the
 * screen going away makes it pointless. It is the wrong owner for a write the user has already
 * committed to: popping a destination clears its ViewModel, which cancels the scope, which cancels
 * the call — so tapping Save and leaving before it returned lost the task with nothing reported.
 *
 * [SupervisorJob] so one failed write cannot take the others down with it.
 */
@Module
@InstallIn(SingletonComponent::class)
object ApplicationScopeModule {

    @Provides
    @Singleton
    @ApplicationScope
    fun provideApplicationScope(
        @DefaultDispatcher dispatcher: CoroutineDispatcher,
    ): CoroutineScope = CoroutineScope(SupervisorJob() + dispatcher)
}
