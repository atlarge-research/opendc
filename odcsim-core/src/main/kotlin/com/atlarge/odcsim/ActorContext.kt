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
 * Represents the context in which the execution of an actor's behavior takes place.
 *
 * @param T The shape of the messages the actor accepts.
 */
interface ActorContext<in T : Any> {
    /**
     * The identity of the actor, bound to the lifecycle of this actor instance.
     */
    val self: ActorRef<T>

    /**
     * The point of time within the simulation.
     */
    val time: Instant

    /**
     * Spawn a child actor from the given [Behavior] and with the specified name.
     *
     * @param name The name of the child actor to spawn.
     * @param behavior The behavior of the child actor to spawn.
     * @return A reference to the child that has/will be spawned.
     */
    fun <U : Any> spawn(name: String, behavior: Behavior<U>): ActorRef<U>

    /**
     * Request the specified child actor to be stopped in asynchronous fashion.
     *
     * @param child The reference to the child actor to stop.
     * @return `true` if the ref points to a child actor, otherwise `false`.
     */
    fun <U : Any> stop(child: ActorRef<U>): Boolean
}
