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
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.DeferredBehavior
import com.atlarge.odcsim.ReceivingBehavior
import com.atlarge.odcsim.SameBehavior
import com.atlarge.odcsim.Signal
import com.atlarge.odcsim.StoppedBehavior
import com.atlarge.odcsim.UnhandledBehavior
import com.atlarge.odcsim.isAlive
import com.atlarge.odcsim.isUnhandled

/**
 * Helper class that interprets messages/signals, canonicalizes special objects and manages the life-cycle of
 * [Behavior] instances.
 *
 * @param initialBehavior The initial behavior to use.
 */
class BehaviorInterpreter<T : Any>(initialBehavior: Behavior<T>) {
    /**
     * The current [Behavior] instance.
     */
    var behavior: Behavior<T> = initialBehavior
        private set

    /**
     * A flag to indicate the interpreter is still alive.
     */
    val isAlive: Boolean get() = behavior.isAlive

    /**
     * Construct a [BehaviorInterpreter] with the specified initial behavior and immediately start it in the specified
     * context.
     *
     * @param initialBehavior The initial behavior of the actor.
     * @param ctx The [ActorContext] to run the behavior in.
     */
    constructor(initialBehavior: Behavior<T>, ctx: ActorContext<T>) : this(initialBehavior) {
        start(ctx)
    }

    /**
     * Start the initial behavior.
     *
     * @param ctx The [ActorContext] to start the behavior in.
     */
    fun start(ctx: ActorContext<T>) {
        behavior = validateAsInitial(start(ctx, behavior))
    }

    /**
     * Stop the current active behavior and move into the stopped state.
     *
     * @param ctx The [ActorContext] this takes place in.
     */
    fun stop(ctx: ActorContext<T>) {
        behavior = start(ctx, StoppedBehavior.narrow())
    }

    /**
     * Replace the current behavior with the specified new behavior.
     *
     * @param ctx The [ActorContext] to run the behavior in.
     * @param next The behavior to replace the current behavior with.
     */
    fun become(ctx: ActorContext<T>, next: Behavior<T>) {
        this.behavior = canonicalize(ctx, behavior, next)
    }

    /**
     * Propagate special states of the wrapper [Behavior] to the specified [Behavior]. This means
     * that if the behavior of this interpreter is stopped or unhandled, this will be propagated.
     *
     * @param behavior The [Behavior] to map.
     * @return Either the specified [Behavior] or the propagated special objects.
     */
    fun propagate(behavior: Behavior<T>): Behavior<T> =
        if (this.behavior.isUnhandled || !this.behavior.isAlive)
            this.behavior
        else
            behavior

    /**
     * Interpret the given message of type [T] using the current active behavior.
     *
     * @return `true` if the message was handled by the active behavior, `false` otherwise.
     */
    fun interpretMessage(ctx: ActorContext<T>, msg: T): Boolean = interpret(ctx, msg, false)

    /**
     * Interpret the given [Signal] using the current active behavior.
     *
     * @return `true` if the signal was handled by the active behavior, `false` otherwise.
     */
    fun interpretSignal(ctx: ActorContext<T>, signal: Signal): Boolean = interpret(ctx, signal, true)

    /**
     * Interpret the given message or signal using the current active behavior.
     *
     * @return `true` if the message or signal was handled by the active behavior, `false` otherwise.
     */
    private fun interpret(ctx: ActorContext<T>, msg: Any, isSignal: Boolean): Boolean =
        if (isAlive) {
            val next = when (val current = behavior) {
                is DeferredBehavior<T> ->
                    throw IllegalStateException("Deferred [$current] should not be passed to interpreter")
                is ReceivingBehavior<T> ->
                    if (isSignal)
                        current.receiveSignal(ctx, msg as Signal)
                    else
                        @Suppress("UNCHECKED_CAST")
                        current.receive(ctx, msg as T)
                is SameBehavior, is UnhandledBehavior ->
                    throw IllegalStateException("Cannot execute with [$current] as behavior")
                is StoppedBehavior -> current
            }

            val unhandled = next.isUnhandled
            behavior = canonicalize(ctx, behavior, next)
            !unhandled
        } else {
            false
        }

    /**
     * Validate whether the given [Behavior] can be used as initial behavior. Throw an [IllegalArgumentException] if
     * the [Behavior] is not valid.
     *
     * @param behavior The behavior to validate.
     */
    private fun validateAsInitial(behavior: Behavior<T>): Behavior<T> =
        when (behavior) {
            is SameBehavior, is UnhandledBehavior ->
                throw IllegalArgumentException("Cannot use [$behavior] as initial behavior")
            else -> behavior
        }

    /**
     * Helper methods to properly manage the special, canned behavior objects. It highly recommended to use the
     * [BehaviorInterpreter] instead to properly manage the life-cycles of the behavior objects.
     */
    companion object {
        /**
         * Start the initial behavior of an actor in the specified [ActorContext].
         *
         * This will activate the initial behavior and canonicalize the resulting behavior.
         *
         * @param ctx The [ActorContext] to start the behavior in.
         * @param behavior The initial behavior to start.
         * @return The behavior that has been started.
         */
        tailrec fun <T : Any> start(ctx: ActorContext<T>, behavior: Behavior<T>): Behavior<T> =
            when (behavior) {
                is DeferredBehavior<T> -> start(ctx, behavior(ctx))
                else -> behavior
            }

        /**
         * Given a possibly special behavior (same or unhandled) and a "current" behavior (which defines the meaning of
         * encountering a `same` behavior) this method computes the next behavior, suitable for passing a message or
         * signal.
         *
         * @param ctx The context in which the actor runs.
         * @param current The actor's current behavior.
         * @param next The actor's next behavior.
         * @return The actor's canonicalized next behavior.
         */
        tailrec fun <T : Any> canonicalize(ctx: ActorContext<T>, current: Behavior<T>, next: Behavior<T>): Behavior<T> =
            when (next) {
                is SameBehavior, current -> current
                is UnhandledBehavior -> current
                is DeferredBehavior<T> -> canonicalize(ctx, current, next(ctx))
                else -> next
            }
    }
}
