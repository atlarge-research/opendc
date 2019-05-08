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

import org.slf4j.Logger

/**
 * Represents the context in which the execution of an actor's behavior takes place.
 *
 * @param T The shape of the messages the actor accepts.
 */
interface ActorContext<T : Any> {
    /**
     * The identity of the actor, bound to the lifecycle of this actor instance.
     */
    val self: ActorRef<T>

    /**
     * The point of time within the simulation.
     */
    val time: Instant

    /**
     * The [ActorSystem] the actor is part of.
     */
    val system: ActorSystem<*>

    /**
     * An actor specific logger instance.
     */
    val log: Logger

    /**
     * Send the specified message to the actor referenced by this [ActorRef].
     *
     * Please note that callees must guarantee that messages are sent strictly in increasing time.
     * If so, this method guarantees that:
     * - A message will never be received earlier than specified
     * - A message might arrive later than specified if the two actors are not synchronized.
     *
     * @param ref The actor to send the message to.
     * @param msg The message to send to the referenced actor.
     * @param after The delay after which the message should be received by the actor.
     */
    fun <U : Any> send(ref: ActorRef<U>, msg: U, after: Duration = 0.0)

    /**
     * Spawn a child actor from the given [Behavior] and with the specified name.
     *
     * @param behavior The behavior of the child actor to spawn.
     * @param name The name of the child actor to spawn.
     * @return A reference to the child that has/will be spawned.
     */
    fun <U : Any> spawn(behavior: Behavior<U>, name: String): ActorRef<U>

    /**
     * Spawn an anonymous child actor from the given [Behavior].
     *
     * @param behavior The behavior of the child actor to spawn.
     * @return A reference to the child that has/will be spawned.
     */
    fun <U : Any> spawnAnonymous(behavior: Behavior<U>): ActorRef<U>

    /**
     * Request the specified child actor to be stopped in asynchronous fashion.
     *
     * @param child The reference to the child actor to stop.
     * @return `true` if the ref points to a child actor, otherwise `false`.
     */
    fun stop(child: ActorRef<*>): Boolean

    /**
     * Watch the specified [ActorRef] for termination of the referenced actor. On termination of the watched actor,
     * a [Terminated] signal is sent to this actor.
     *
     * @param target The target actor to watch.
     */
    fun watch(target: ActorRef<*>)

    /**
     * Revoke the registration established by [watch].
     *
     * In case there exists no registration for the specified [target], no action will be performed.
     *
     * @param target The target actor to unwatch.
     */
    fun unwatch(target: ActorRef<*>)

    /**
     * Synchronize the local virtual time of this target with the other referenced actor's local virtual time.
     *
     * By default, actors are not guaranteed to be synchronized, meaning that for some implementations, virtual time may
     * drift between different actors. Synchronization between two actors ensures that virtual time remains consistent
     * between at least the two actors.
     *
     * Be aware that this method may cause a jump in virtual time in order to get consistent with [target].
     * Furthermore, please note that synchronization might incur performance degradation and should only be used
     * when necessary.
     *
     * @param target The reference to the target actor to synchronize with.
     */
    fun sync(target: ActorRef<*>)

    /**
     * Desynchronize virtual time between two actors if possible.
     *
     * Please note that this method only provides a hint to the [ActorSystem] that it may drop synchronization between
     * the actors, but [ActorSystem] is not compelled to actually do so (i.e. in the case where synchronization is
     * always guaranteed).
     *
     * Furthermore, if [target] is already desychronized, the method should return without error. [ActorContext.isSync]
     * may be used to determine if an actor is synchronized.
     *
     * @param target The reference to the target actor to desynchronize with.
     */
    fun unsync(target: ActorRef<*>)

    /**
     * Determine whether this actor and [target] are synchronized in virtual time.
     *
     * @param target The target to check for synchronization.
     * @return `true` if [target] is synchronized with this actor, `false` otherwise.
     */
    fun isSync(target: ActorRef<*>): Boolean
}
