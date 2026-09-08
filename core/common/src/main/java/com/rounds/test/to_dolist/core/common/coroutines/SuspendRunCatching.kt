package com.rounds.test.to_dolist.core.common.coroutines

import kotlin.coroutines.cancellation.CancellationException

/**
 * [runCatching] swallows [CancellationException], which turns a cancelled coroutine into a fake
 * failure and leaves the scope in an inconsistent state. Every data-layer call goes through this
 * instead.
 */
inline fun <T> suspendRunCatching(block: () -> T): Result<T> = try {
    Result.success(block())
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (throwable: Throwable) {
    Result.failure(throwable)
}
