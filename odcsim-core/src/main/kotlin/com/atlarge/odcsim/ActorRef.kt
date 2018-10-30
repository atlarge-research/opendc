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
 * A reference to an entity in simulation that accepts messages of type [T].
 */
interface ActorRef<in T : Any> {
    /**
     * The path for this actor (from this actor up to the root actor).
     */
    val path: ActorPath

    /**
     * Send the specified message to the actor referenced by this [ActorRef].
     *
     * @param msg The message to send to the referenced architecture.
     * @param after The delay after which the message should be received by the actor.
     */
    fun send(msg: T, after: Duration = 0.1)
}

/**
 * Unsafe helper method for widening the type accepted by this [ActorRef].
 */
@Suppress("UNCHECKED_CAST")
fun <U : Any, T : U> ActorRef<T>.upcast(): ActorRef<U> = this as ActorRef<U>
