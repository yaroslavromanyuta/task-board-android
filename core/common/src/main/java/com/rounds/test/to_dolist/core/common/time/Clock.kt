package com.rounds.test.to_dolist.core.common.time

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Instant
import javax.inject.Inject
import javax.inject.Singleton

/** Abstracted so `createdAt` is assertable in tests instead of "whatever the wall clock said". */
interface Clock {
    fun now(): Instant
}

@Singleton
class SystemClock @Inject constructor() : Clock {
    override fun now(): Instant = Instant.now()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class ClockModule {

    @Binds
    @Singleton
    abstract fun bindClock(impl: SystemClock): Clock
}
