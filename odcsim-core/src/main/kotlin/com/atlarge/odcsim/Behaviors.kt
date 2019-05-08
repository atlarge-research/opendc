/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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
@file:JvmName("Behaviors")
package com.atlarge.odcsim

import com.atlarge.odcsim.internal.BehaviorInterpreter
import com.atlarge.odcsim.internal.EmptyBehavior
import com.atlarge.odcsim.internal.IgnoreBehavior
import com.atlarge.odcsim.internal.TimerSchedulerImpl
import com.atlarge.odcsim.internal.sendSignal

/**
 * This [Behavior] is used to signal that this actor shall terminate voluntarily. If this actor has created child actors
 * then these will be stopped as part of the shutdown procedure.
 */
fun <T : Any> stopped(): Behavior<T> = StoppedBehavior.unsafeCast()

/**
 * This [Behavior] is used to signal that this actor wants to reuse its previous behavior.
 */
fun <T : Any> same(): Behavior<T> = SameBehavior.unsafeCast()

/**
 * This [Behavior] is used to signal to the system that the last message or signal went unhandled. This will
 * reuse the previous behavior.
 */
fun <T : Any> unhandled(): Behavior<T> = UnhandledBehavior.unsafeCast()

/**
 * A factory for [Behavior]. Creation of the behavior instance is deferred until the actor is started.
 */
fun <T : Any> setup(block: (ActorContext<T>) -> Behavior<T>): Behavior<T> {
    return object : DeferredBehavior<T>() {
        override fun invoke(ctx: ActorContext<T>): Behavior<T> = block(ctx)
    }
}

/**
 * A [Behavior] that ignores any incoming message or signal and keeps the same behavior.
 */
fun <T : Any> ignore(): Behavior<T> = IgnoreBehavior.narrow()

/**
 * A [Behavior] that treats every incoming message or signal as unhandled.
 */
fun <T : Any> empty(): Behavior<T> = EmptyBehavior.narrow()

/**
 * Construct a [Behavior] that reacts to incoming messages, provides access to the [ActorContext] and returns the
 * actor's next behavior.
 */
fun <T : Any> receive(handler: (ActorContext<T>, T) -> Behavior<T>): Behavior<T> {
    return object : ReceivingBehavior<T>() {
        override fun receive(ctx: ActorContext<T>, msg: T): Behavior<T> = handler(ctx, msg)
    }
}

/**
 * Construct a [Behavior] that reacts to incoming messages of type [U], provides access to the [ActorContext] and
 * returns the actor's next behavior. Other messages will be unhandled.
 */
inline fun <T : Any, reified U : T> receiveOf(crossinline handler: (ActorContext<T>, U) -> Behavior<T>): Behavior<T> {
    return object : ReceivingBehavior<T>() {
        override fun receive(ctx: ActorContext<T>, msg: T): Behavior<T> {
            return if (msg is U)
                handler(ctx, msg)
            else
                unhandled()
        }
    }
}

/**
 * Construct a [Behavior] that reacts to incoming messages and returns the actor's next behavior.
 */
fun <T : Any> receiveMessage(handler: (T) -> Behavior<T>): Behavior<T> {
    return object : ReceivingBehavior<T>() {
        override fun receive(ctx: ActorContext<T>, msg: T): Behavior<T> = handler(msg)
    }
}

/**
 * Construct a [Behavior] that reacts to incoming signals, provides access to the [ActorContext] and returns the
 * actor's next behavior.
 */
fun <T : Any> receiveSignal(handler: (ActorContext<T>, Signal) -> Behavior<T>): Behavior<T> {
    return object : ReceivingBehavior<T>() {
        override fun receiveSignal(ctx: ActorContext<T>, signal: Signal): Behavior<T> = handler(ctx, signal)
    }
}

/**
 * Construct a [Behavior] that wraps another behavior instance and uses a [BehaviorInterpreter] to pass incoming
 * messages and signals to the wrapper behavior.
 */
fun <T : Any> wrap(behavior: Behavior<T>, wrap: (BehaviorInterpreter<T>) -> Behavior<T>): Behavior<T> {
    return setup { ctx -> wrap(BehaviorInterpreter(behavior, ctx)) }
}

/**
 * Obtain a [TimerScheduler] for building a [Behavior] instance.
 */
fun <T : Any> withTimers(handler: (TimerScheduler<T>) -> Behavior<T>): Behavior<T> {
    return setup { ctx ->
        val scheduler = TimerSchedulerImpl(ctx)
        receiveSignal<T> { _, signal ->
            if (signal is TimerSchedulerImpl.TimerSignal) {
                val res = scheduler.interceptTimerSignal(signal)
                if (res != null) {
                    ctx.send(ctx.self, res)
                    return@receiveSignal same()
                }
            }
            unhandled()
        }.join(handler(scheduler))
    }
}

/**
 * Construct a [Behavior] that waits for the specified duration before constructing the next behavior.
 *
 * @param after The delay before constructing the next behavior.
 * @param handler The handler to construct the behavior with.
 */
fun <T : Any> withTimeout(after: Duration, handler: (ActorContext<T>) -> Behavior<T>): Behavior<T> =
    setup { ctx ->
        val target = Any()
        ctx.sendSignal(ctx.self, Timeout(target), after)
        receiveSignal { _, signal ->
            if (signal is Timeout && signal.target == target) {
                handler(ctx)
            } else {
                unhandled()
            }
        }
    }

/**
 * Join together both [Behavior] with another [Behavior], essentially running them side-by-side, only directly
 * propagating stopped behavior.
 *
 * @param that The behavior to join with.
 */
fun <T : Any> Behavior<T>.join(that: Behavior<T>): Behavior<T> =
    wrap(this) { left ->
        wrap(that) { right ->
            object : ReceivingBehavior<T>() {
                override fun receive(ctx: ActorContext<T>, msg: T): Behavior<T> {
                    if (left.interpretMessage(ctx, msg)) {
                        return left.propagate(this) // Propagate stopped behavior
                    } else if (right.interpretMessage(ctx, msg)) {
                        return right.propagate(this)
                    }

                    return unhandled()
                }

                override fun receiveSignal(ctx: ActorContext<T>, signal: Signal): Behavior<T> {
                    if (left.interpretSignal(ctx, signal)) {
                        return left.propagate(this)
                    } else if (right.interpretSignal(ctx, signal)) {
                        return right.propagate(this)
                    }

                    return unhandled()
                }
            }
        }
    }

/**
 * Widen the type of messages the [Behavior] by marking all other messages as unhandled.
 */
inline fun <U : Any, reified T : U> Behavior<T>.widen(): Behavior<U> = widen {
    if (it is T)
        it
    else
        null
}

/**
 * Keep the specified [Behavior] alive if it returns the stopped behavior.
 */
fun <T : Any> Behavior<T>.keepAlive(): Behavior<T> =
    wrap(this) { interpreter ->
        object : ReceivingBehavior<T>() {
            override fun receive(ctx: ActorContext<T>, msg: T): Behavior<T> {
                if (interpreter.interpretMessage(ctx, msg)) {
                    return this
                }
                return empty()
            }

            override fun receiveSignal(ctx: ActorContext<T>, signal: Signal): Behavior<T> {
                if (interpreter.interpretSignal(ctx, signal)) {
                    return this
                }

                return empty()
            }
        }
    }
