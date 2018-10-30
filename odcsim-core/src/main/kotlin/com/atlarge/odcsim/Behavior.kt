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

package com.atlarge.odcsim

import com.atlarge.odcsim.dsl.wrap

/**
 * The representation of the behavior of an actor.
 *
 * Behavior can be formulated using factory methods on the companion object or by extending either [DeferredBehavior] or
 * [ReceivingBehavior].
 *
 * Users are advised not to close over [ActorContext] within [Behavior], as it will causes it to become immobile,
 * meaning it cannot be moved to another context and executed there, and therefore it cannot be replicated or forked
 * either.
 *
 * @param T The shape of the messages the behavior accepts.
 */
sealed class Behavior<T : Any> {
    /**
     * Narrow the type of this behavior.
     *
     * This is almost always a safe operation, but might cause [ClassCastException] in case a narrowed behavior sends
     * messages of a different type to itself and is chained via [Behavior.orElse].
     */
    fun <U : T> narrow(): Behavior<U> {
        @Suppress("UNCHECKED_CAST")
        return this as Behavior<U>
    }

    /**
     * Companion object used to bind factory methods to.
     */
    companion object {
        /**
         * This [Behavior] is used to signal that this actor shall terminate voluntarily. If this actor has created child actors
         * then these will be stopped as part of the shutdown procedure.
         */
        fun <T : Any> stopped(): Behavior<T> {
            @Suppress("UNCHECKED_CAST")
            return StoppedBehavior as Behavior<T>
        }

        /**
         * This [Behavior] is used to signal that this actor wants to reuse its previous behavior.
         */
        fun <T : Any> same(): Behavior<T> {
            @Suppress("UNCHECKED_CAST")
            return SameBehavior as Behavior<T>
        }

        /**
         * This [Behavior] is used to signal to the system that the last message or signal went unhandled. This will
         * reuse the previous behavior.
         */
        fun <T : Any> unhandled(): Behavior<T> {
            @Suppress("UNCHECKED_CAST")
            return UnhandledBehavior as Behavior<T>
        }
    }
}

/**
 * A [Behavior] that defers the construction of the actual [Behavior] until the actor is started in some [ActorContext].
 * If the actor is already started, it will immediately evaluate.
 *
 * @param T The shape of the messages the behavior accepts.
 */
abstract class DeferredBehavior<T : Any> : Behavior<T>() {
    /**
     * Create a [Behavior] instance in the [specified][ctx] [ActorContext].
     *
     * @param ctx The [ActorContext] in which the behavior runs.
     * @return The actor's next behavior.
     */
    abstract operator fun invoke(ctx: ActorContext<T>): Behavior<T>
}

/**
 * A [Behavior] that concretely defines how an actor will react to the messages and signals it receives.
 * The message may either be of the type that the actor declares and which is part of the [ActorRef] signature,
 * or it may be a system [Signal] that expresses a lifecycle event of either this actor or one of its child actors.
 *
 * @param T The shape of the messages the behavior accepts.*
 */
abstract class ReceivingBehavior<T : Any> : Behavior<T>() {
    /**
     * Process an incoming message of type [T] and return the actor's next behavior.
     *
     * The returned behavior can in addition to normal behaviors be one of the canned special objects:
     * - returning [Behavior.Companion.stopped] will terminate this Behavior
     * - returning [Behavior.Companion.same] designates to reuse the current Behavior
     * - returning [Behavior.Companion.unhandled] keeps the same Behavior and signals that the message was not yet handled
     *
     * @param ctx The [ActorContext] in which the actor is currently running.
     * @param msg The message that was received.
     */
    open fun receive(ctx: ActorContext<T>, msg: T): Behavior<T> = Behavior.unhandled()

    /**
     * Process an incoming [Signal] and return the actor's next behavior.
     *
     * The returned behavior can in addition to normal behaviors be one of the canned special objects:
     * - returning [Behavior.Companion.stopped] will terminate this Behavior
     * - returning [Behavior.Companion.same] designates to reuse the current Behavior
     * - returning [Behavior.Companion.unhandled] keeps the same Behavior and signals that the message was not yet handled
     *
     * @param ctx The [ActorContext] in which the actor is currently running.
     * @param signal The [Signal] that was received.
     */
    open fun receiveSignal(ctx: ActorContext<T>, signal: Signal): Behavior<T> = Behavior.unhandled()
}

/**
 * Compose this [Behavior] with a fallback [Behavior] which is used in case this [Behavior] does not handle the incoming
 * message or signal.
 */
fun <T : Any> Behavior<T>.orElse(behavior: Behavior<T>): Behavior<T> =
    Behavior.wrap(this) { left ->
        Behavior.wrap(behavior) { right ->
            object : ReceivingBehavior<T>() {
                override fun receive(ctx: ActorContext<T>, msg: T): Behavior<T> {
                    if (left.interpretMessage(ctx, msg)) {
                        return left.behavior
                    } else if (right.interpretMessage(ctx, msg)) {
                        return right.behavior
                    }

                    return Behavior.unhandled()
                }

                override fun receiveSignal(ctx: ActorContext<T>, signal: Signal): Behavior<T> {
                    if (left.interpretSignal(ctx, signal)) {
                        return left.behavior
                    } else if (right.interpretSignal(ctx, signal)) {
                        return right.behavior
                    }

                    return Behavior.unhandled()
                }
            }
        }
    }

/**
 * A flag to indicate whether a [Behavior] instance is still alive.
 */
val <T : Any> Behavior<T>.isAlive get() = this !is StoppedBehavior

/**
 * A flag to indicate whether the last message/signal went unhandled.
 */
val <T : Any> Behavior<T>.isUnhandled get() = this !is UnhandledBehavior

/**
 * A special [Behavior] instance that signals that the actor has stopped.
 */
private object StoppedBehavior : Behavior<Any>() {
    override fun toString() = "Stopped"
}

/**
 * A special [Behavior] object to signal that the actor wants to reuse its previous behavior.
 */
private object SameBehavior : Behavior<Nothing>() {
    override fun toString() = "Same"
}

/**
 * A special [Behavior] object that indicates that the last message or signal was not handled.
 */
private object UnhandledBehavior : Behavior<Nothing>() {
    override fun toString() = "Unhandled"
}

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

    /**
     * A flag to indicate whether the current behavior is still alive.
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
        behavior = validateAsInitial(start(behavior, ctx))
    }

    /**
     * Stop the current active behavior and move into the stopped state.
     *
     * @param ctx The [ActorContext] this takes place in.
     */
    fun stop(ctx: ActorContext<T>) {
        behavior = start(StoppedBehavior.narrow(), ctx)
    }

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
            behavior = canonicalize(next, behavior, ctx)
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
         * @param behavior The initial behavior to start.
         * @param ctx The [ActorContext] to start the behavior in.
         * @return The behavior that has been started.
         */
        tailrec fun <T : Any> start(behavior: Behavior<T>, ctx: ActorContext<T>): Behavior<T> =
            when (behavior) {
                is DeferredBehavior<T> -> start(behavior(ctx), ctx)
                else -> behavior
            }

        /**
         * Given a possibly special behavior (same or unhandled) and a "current" behavior (which defines the meaning of
         * encountering a `same` behavior) this method computes the next behavior, suitable for passing a message or
         * signal.
         *
         * @param behavior The actor's next behavior.
         * @param current The actor's current behavior.
         * @param ctx The context in which the actor runs.
         * @return The actor's canonicalized next behavior.
         */
        tailrec fun <T : Any> canonicalize(behavior: Behavior<T>, current: Behavior<T>, ctx: ActorContext<T>): Behavior<T> =
            when (behavior) {
                is SameBehavior, current -> current
                is UnhandledBehavior -> current
                is DeferredBehavior<T> -> canonicalize(behavior(ctx), current, ctx)
                else -> behavior
            }
    }
}
