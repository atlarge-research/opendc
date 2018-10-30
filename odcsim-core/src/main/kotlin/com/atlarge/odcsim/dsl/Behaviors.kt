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

package com.atlarge.odcsim.dsl

import com.atlarge.odcsim.ActorContext
import com.atlarge.odcsim.Behavior
import com.atlarge.odcsim.BehaviorInterpreter
import com.atlarge.odcsim.DeferredBehavior
import com.atlarge.odcsim.ReceivingBehavior
import com.atlarge.odcsim.Signal

/**
 * A factory for [Behavior]. Creation of the behavior instance is deferred until the actor is started.
 */
fun <T : Any> Behavior.Companion.setup(block: (ActorContext<T>) -> Behavior<T>): Behavior<T> {
    return object : DeferredBehavior<T>() {
        override fun invoke(ctx: ActorContext<T>): Behavior<T> = block(ctx)
    }
}

/**
 * A [Behavior] that ignores any incoming message or signal and keeps the same behavior.
 */
fun <T : Any> Behavior.Companion.ignore(): Behavior<T> = IgnoreBehavior.narrow()

/**
 * A [Behavior] object that ignores all messages sent to the actor.
 */
private object IgnoreBehavior : ReceivingBehavior<Any>() {
    override fun receive(ctx: ActorContext<Any>, msg: Any): Behavior<Any> = this

    override fun receiveSignal(ctx: ActorContext<Any>, signal: Signal): Behavior<Any> = this

    override fun toString() = "Ignore"
}

/**
 * A [Behavior] that treats every incoming message or signal as unhandled.
 */
fun <T : Any> Behavior.Companion.empty(): Behavior<T> = EmptyBehavior.narrow()

/**
 * A [Behavior] object that does not handle any message it receives.
 */
private object EmptyBehavior : ReceivingBehavior<Any>() {
    override fun toString() = "Empty"
}

/**
 * Construct a [Behavior] that reacts to incoming messages, provides access to the [ActorContext] and returns the
 * actor's next behavior.
 */
fun <T : Any> Behavior.Companion.receive(handler: (ActorContext<T>, T) -> Behavior<T>): Behavior<T> {
    return object : ReceivingBehavior<T>() {
        override fun receive(ctx: ActorContext<T>, msg: T): Behavior<T> = handler(ctx, msg)
    }
}

/**
 * Construct a [Behavior] that reacts to incoming messages and returns the actor's next behavior.
 */
fun <T : Any> Behavior.Companion.receiveMessage(onMessage: (T) -> Behavior<T>): Behavior<T> {
    return object : ReceivingBehavior<T>() {
        override fun receive(ctx: ActorContext<T>, msg: T): Behavior<T> = onMessage(msg)
    }
}

/**
 * Construct a [Behavior] that reacts to incoming signals, provides access to the [ActorContext] and returns the
 * actor's next behavior.
 */
fun <T : Any> Behavior.Companion.receiveSignal(handler: (ActorContext<T>, Signal) -> Behavior<T>): Behavior<T> {
    return object : ReceivingBehavior<T>() {
        override fun receiveSignal(ctx: ActorContext<T>, signal: Signal): Behavior<T> = handler(ctx, signal)
    }
}

/**
 * Construct a [Behavior] that wraps another behavior instance and uses a [BehaviorInterpreter] to pass incoming
 * messages and signals to the wrapper behavior.
 */
fun <T : Any> Behavior.Companion.wrap(behavior: Behavior<T>, wrap: (BehaviorInterpreter<T>) -> Behavior<T>): Behavior<T> {
    return Behavior.setup { ctx -> wrap(BehaviorInterpreter(behavior, ctx)) }
}
