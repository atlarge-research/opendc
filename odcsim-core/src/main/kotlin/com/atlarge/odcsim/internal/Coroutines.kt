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

package com.atlarge.odcsim.internal

import com.atlarge.odcsim.ActorContext
import com.atlarge.odcsim.ActorRef
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.Instant
import com.atlarge.odcsim.ReceivingBehavior
import com.atlarge.odcsim.Signal
import com.atlarge.odcsim.coroutines.SuspendingActorContext
import com.atlarge.odcsim.coroutines.SuspendingBehavior
import com.atlarge.odcsim.dsl.receiveMessage
import com.atlarge.odcsim.dsl.receiveSignal
import com.atlarge.odcsim.coroutines.suspendWithBehavior
import kotlin.coroutines.Continuation
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.resume
import kotlin.coroutines.startCoroutine

/**
 * The interface that is exposed from the [CoroutineContext] and provides control over the [SuspendingBehavior]
 * instance.
 */
interface SuspendingBehaviorContext<T : Any> : CoroutineContext.Element {
    /**
     * The [ActorContext] in which the actor currently runs.
     */
    val context: SuspendingActorContext<T>

    /**
     * The current active behavior
     */
    val behavior: Behavior<T>

    /**
     * Replace the current active behavior with the specified new behavior.
     *
     * @param next The behavior to replace the current behavior with.
     */
    fun become(next: Behavior<T>)

    /**
     * This key provides users access to an untyped actor context in case the coroutine runs inside a
     * [SuspendingBehavior].
     */
    companion object Key : CoroutineContext.Key<SuspendingBehaviorContext<*>>
}

/**
 * Implementation of [SuspendingBehavior] class that maps the suspending method calls to the [Behavior]
 * interface.
 * This implementation uses the fact that each actor is thread-safe (as it processes its mailbox sequentially).
 */
internal class SuspendingBehaviorImpl<T : Any>(actorContext: ActorContext<T>, initialBehavior: SuspendingBehavior<T>) :
        ReceivingBehavior<T>(), SuspendingActorContext<T>, SuspendingBehaviorContext<T> {
    /**
     * The next behavior to use.
     */
    private var next: Behavior<T> = this

    /**
     * The [BehaviorInterpreter] to wrap the suspending behavior.
     */
    private val interpreter = BehaviorInterpreter(initialBehavior)

    /**
     * The current active [ActorContext].
     */
    private var actorContext: ActorContext<T>

    init {
        this.actorContext = actorContext
        this.start()
    }

    override fun receive(ctx: ActorContext<T>, msg: T): Behavior<T> {
        this.actorContext = ctx
        return interpreter.also { it.interpretMessage(ctx, msg) }.propagate(next)
    }

    override fun receiveSignal(ctx: ActorContext<T>, signal: Signal): Behavior<T> {
        this.actorContext = ctx
        return interpreter.also { it.interpretSignal(ctx, signal) }.propagate(next)
    }

    override val self: ActorRef<T> get() = actorContext.self

    override val time: Instant get() = actorContext.time

    override fun <U : Any> spawn(behavior: Behavior<U>, name: String) = actorContext.spawn(behavior, name)

    override fun stop(child: ActorRef<*>): Boolean = actorContext.stop(child)

    override suspend fun receive(): T = suspendWithBehavior<T, T> { cont, next ->
        receiveMessage { msg ->
            cont.resume(msg)
            next()
        }
    }

    override suspend fun receiveSignal(): Signal = suspendWithBehavior<T, Signal> { cont, next ->
        receiveSignal { _, signal ->
            cont.resume(signal)
            next()
        }
    }

    override val context: SuspendingActorContext<T> get() = this

    override val behavior: Behavior<T> get() = interpreter.behavior

    override fun become(next: Behavior<T>) {
        interpreter.become(actorContext, next)
    }

    override val key: CoroutineContext.Key<*> = SuspendingBehaviorContext.Key

    /**
     * Start the suspending behavior.
     */
    private fun start() {
        val behavior = interpreter.behavior as SuspendingBehavior<T>
        val block: suspend () -> Unit = { behavior(this) }
        block.startCoroutine(SuspendingBehaviorImplContinuation())
    }

    /**
     * Stop the suspending behavior.
     */
    private fun stop() {
        this.interpreter.stop(actorContext)
    }

    /**
     * The continuation of suspending behavior.
     */
    private inner class SuspendingBehaviorImplContinuation : Continuation<Unit> {
        override val context = this@SuspendingBehaviorImpl

        override fun resumeWith(result: Result<Unit>) {
            // TODO Add some form of error handling here
            stop()
            next = Behavior.stopped()
        }
    }
}
