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
 * An actor system is a hierarchical grouping of actors that represents a discrete event simulation.
 *
 * An implementation of this interface should be provided by an engine. See for example *odcsim-engine-omega*,
 * which is the reference implementation of the *odcsim* API.
 *
 * @param T The shape of the messages the root actor in the system can receive.
 */
interface ActorSystem<in T : Any> : ActorRef<T> {
    /**
     * The current point in simulation time.
     */
    val time: Instant

    /**
     * The name of this engine instance, used to distinguish between multiple engines running within the same JVM.
     */
    val name: String

    /**
     * Run the actors until the specified point in simulation time.
     *
     * @param until The point until which the simulation should run.
     */
    fun run(until: Duration = Duration.POSITIVE_INFINITY)

    /**
     * Send the specified message to the root actor of this [ActorSystem].
     *
     * @param msg The message to send to the referenced actor.
     * @param after The delay after which the message should be received by the actor.
     */
    fun send(msg: T, after: Duration = 0.1)

    /**
     * Terminates this actor system.
     *
     * This will stop the root actor and in turn will recursively stop all its child actors.
     *
     * This is an asynchronous operation.
     */
    fun terminate()
}
