/*
 * MIT License
 *
 * Copyright (c) 2018 atlarge-research
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.atlarge.odcsim.coroutines

import com.atlarge.odcsim.ActorContext
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.DeferredBehavior
import com.atlarge.odcsim.Signal
import com.atlarge.odcsim.internal.SuspendingActorContextImpl
import com.atlarge.odcsim.internal.SuspendingBehaviorImpl
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.suspendCoroutine
import kotlin.coroutines.intrinsics.suspendCoroutineUninterceptedOrReturn

/**
 * A [Behavior] that allows method calls to suspend execution via Kotlin coroutines.
 *
 * @param T The shape of the messages the actor accepts.
 */
abstract class SuspendingBehavior<T : Any> : DeferredBehavior<T>() {
    /**
     * Run the suspending logic of this behavior.
     *
     * @param ctx The [SuspendingActorContext] in which the behavior is executed.
     * @return The next behavior for the actor.
     */
    abstract suspend operator fun invoke(ctx: SuspendingActorContext<T>): Behavior<T>

    // Immediately transfer to implementation
    override fun invoke(ctx: ActorContext<T>): Behavior<T> = SuspendingBehaviorImpl(ctx, this).start()
}

/**
 * An [ActorContext] that provides additional functionality for receiving messages and signals from
 * the actor's mailbox.
 *
 * @param T The shape of the messages the actor accepts.
 */
interface SuspendingActorContext<T : Any> : ActorContext<T>, CoroutineContext.Element {
    /**
     * Suspend execution of the active coroutine to wait for a message of type [T] to be received in the actor's
     * mailbox. During suspension, incoming signals will be marked unhandled.
     *
     * @return The message of type [T] that has been received.
     */
    suspend fun receive(): T

    /**
     * Suspend execution of the active coroutine to wait for a [Signal] to be received in the actor's mailbox.
     * During suspension, incoming messages will be marked unhandled.
     *
     * @return The [Signal] that has been received.
     */
    suspend fun receiveSignal(): Signal

    /**
     * A key to provide access to the untyped [SuspendingActorContext] via [CoroutineContext] for suspending methods
     * running inside a [SuspendingBehavior].
     */
    companion object Key : CoroutineContext.Key<SuspendingActorContext<*>>
}

/**
 * Obtains the current continuation instance inside suspend functions and suspends currently running coroutine. [block]
 * should return a [Behavior] that will resume the continuation and return the next behavior which is supplied via the
 * second argument of the block.
 */
suspend fun <T : Any, U> suspendWithBehavior(block: (Continuation<U>, () -> Behavior<T>) -> Behavior<T>): U =
    suspendCoroutine { cont ->
        @Suppress("UNCHECKED_CAST")
        val ctx = cont.context[SuspendingActorContext] as? SuspendingActorContextImpl<T>
            ?: throw UnsupportedOperationException("Coroutine does not run inside SuspendingBehavior")
        ctx.become(block(cont) { ctx.behavior })
    }

/**
 * Obtain the current [SuspendingActorContext] instance for the active continuation.
 */
suspend fun <T : Any> actorContext(): SuspendingActorContext<T> =
    suspendCoroutineUninterceptedOrReturn { cont ->
        @Suppress("UNCHECKED_CAST")
        cont.context[SuspendingActorContext] as? SuspendingActorContext<T>
            ?: throw UnsupportedOperationException("Coroutine does not run inside SuspendingBehavior")
    }

/**
 * Construct a [Behavior] that uses Kotlin coroutines functionality to handle incoming messages and signals.
 */
fun <T : Any> suspending(block: suspend (SuspendingActorContext<T>) -> Behavior<T>): Behavior<T> {
    return object : SuspendingBehavior<T>() {
        override suspend fun invoke(ctx: SuspendingActorContext<T>) = block(ctx)
    }
}
