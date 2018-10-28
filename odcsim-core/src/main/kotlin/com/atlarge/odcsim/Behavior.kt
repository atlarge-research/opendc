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
 * The behavior of an actor defines how it reacts to the messages that it receives.
 * The message may either be of the type that the actor declares and which is part of the [ActorRef] signature,
 * or it may be a system [Signal] that expresses a lifecycle event of either this actor or one of its child actors.
 *
 * @param T The shape of the messages the behavior accepts.
 */
interface Behavior<T : Any> {
    /**
     * Process an incoming message and return the actor's next [Behavior].
     */
    fun receive(ctx: ActorContext<T>, msg: T): Behavior<T> = this

    /**
     * Process an incoming [Signal] and return the actor's next [Behavior].
     */
    fun receiveSignal(ctx: ActorContext<T>, signal: Signal): Behavior<T> = this
}
