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
     * Compose this [Behavior] with a fallback [Behavior] which is used in case this [Behavior] does not handle the
     * incoming message or signal.
     */
    fun orElse(behavior: Behavior<T>): Behavior<T> =
        wrap(this) { left ->
            wrap(behavior) { right ->
                object : ReceivingBehavior<T>() {
                    override fun receive(ctx: ActorContext<T>, msg: T): Behavior<T> {
                        if (left.interpretMessage(ctx, msg)) {
                            return left.behavior
                        } else if (right.interpretMessage(ctx, msg)) {
                            return right.behavior
                        }

                        return unhandled()
                    }

                    override fun receiveSignal(ctx: ActorContext<T>, signal: Signal): Behavior<T> {
                        if (left.interpretSignal(ctx, signal)) {
                            return left.behavior
                        } else if (right.interpretSignal(ctx, signal)) {
                            return right.behavior
                        }

                        return unhandled()
                    }
                }
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
 * @param T The shape of the messages the behavior accepts.
 */
abstract class ReceivingBehavior<T : Any> : Behavior<T>() {
    /**
     * Process an incoming message of type [T] and return the actor's next behavior.
     *
     * The returned behavior can in addition to normal behaviors be one of the canned special objects:
     * - returning [stopped] will terminate this Behavior
     * - returning [same] designates to reuse the current Behavior
     * - returning [unhandled] keeps the same Behavior and signals that the message was not yet handled
     *
     * @param ctx The [ActorContext] in which the actor is currently running.
     * @param msg The message that was received.
     */
    open fun receive(ctx: ActorContext<T>, msg: T): Behavior<T> = unhandled()

    /**
     * Process an incoming [Signal] and return the actor's next behavior.
     *
     * The returned behavior can in addition to normal behaviors be one of the canned special objects:
     * - returning [stopped] will terminate this Behavior
     * - returning [same] designates to reuse the current Behavior
     * - returning [unhandled] keeps the same Behavior and signals that the message was not yet handled
     *
     * @param ctx The [ActorContext] in which the actor is currently running.
     * @param signal The [Signal] that was received.
     */
    open fun receiveSignal(ctx: ActorContext<T>, signal: Signal): Behavior<T> = unhandled()
}

/**
 * A flag to indicate whether a [Behavior] instance is still alive.
 */
val <T : Any> Behavior<T>.isAlive get() = this !is StoppedBehavior

/**
 * A flag to indicate whether the last message/signal went unhandled.
 */
val <T : Any> Behavior<T>.isUnhandled get() = this !is UnhandledBehavior

// The special behaviors are kept in this file as to be able to seal the Behavior class to prevent users from extending
// it.
/**
 * A special [Behavior] instance that signals that the actor has stopped.
 */
internal object StoppedBehavior : Behavior<Any>() {
    override fun toString() = "Stopped"
}

/**
 * A special [Behavior] object to signal that the actor wants to reuse its previous behavior.
 */
internal object SameBehavior : Behavior<Nothing>() {
    override fun toString() = "Same"
}

/**
 * A special [Behavior] object that indicates that the last message or signal was not handled.
 */
internal object UnhandledBehavior : Behavior<Nothing>() {
    override fun toString() = "Unhandled"
}
