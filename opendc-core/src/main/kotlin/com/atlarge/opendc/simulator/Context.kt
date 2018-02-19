/*
 * MIT License
 *
 * Copyright (c) 2017 atlarge-research
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

package com.atlarge.opendc.simulator

import java.util.*
import kotlin.coroutines.experimental.CoroutineContext

/**
 * This interface provides a context for simulation of [Entity] instances, by defining the environment in which the
 * simulation is run and provides means of communicating with other entities in the environment and control its own
 * behaviour.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Context<S, M> : CoroutineContext.Element {
    /**
     * The model of simulation in which the entity exists.
     */
    val model: M

    /**
     * The current point in simulation time.
     */
    val time: Instant

    /**
     * The duration between the current point in simulation time and the last point in simulation time where the
     * [Entity] has done some work. This means the `run()` co-routine has been resumed.
     */
    val delta: Duration

    /**
     * The [Entity] associated with this context.
     */
    val self: Entity<S, M>

    /**
     * The sender of the last received message or `null` in case the process has not received any messages yet.
     *
     * Note that this property is only guaranteed to be correct when accessing after a single suspending call. Methods
     * like `hold()` and `interrupt()` may change the value of this property.
     */
    val sender: Entity<*, *>?

    /**
     * The observable state of the entity bound to this scope.
     */
    var state: S

    /**
     * The observable state of an [Entity] in simulation, which is provided by the simulation context.
     */
    val <E : Entity<S, *>, S> E.state: S

    /**
     * Interrupt an [Entity] process in simulation.
     *
     * @see [Entity.interrupt(Interrupt)]
     * @param reason The reason for interrupting the entity.
     */
    suspend fun Entity<*, *>.interrupt(reason: String) = interrupt(Interrupt(reason))

    /**
     * Interrupt an [Entity] process in simulation.
     *
     * If an [Entity] process has been suspended, the suspending call will throw an [Interrupt] object as a result of
     * this call.
     * Make sure the [Entity] process actually has error handling in place, so it won't take down the whole [Entity]
     * process as result of the interrupt.
     *
     * @param interrupt The interrupt to throw at the entity.
     */
    suspend fun Entity<*, *>.interrupt(interrupt: Interrupt)

    /**
     * Suspend the [Context] of the [Entity] in simulation for the given duration of simulation time before resuming
     * execution and drop all messages that are received during this period.
     *
     * A call to this method will not make the [Context] sleep for the actual duration of time, but instead suspend
     * the process until the no more messages at an earlier point in time have to be processed.
     *
     * @param duration The duration of simulation time to hold before resuming execution.
     */
    suspend fun hold(duration: Duration)

    /**
     * Suspend the [Context] of the [Entity] in simulation for the given duration of simulation time before resuming
     * execution and push all messages that are received during this period to the given queue.
     *
     * A call to this method will not make the [Context] sleep for the actual duration of time, but instead suspend
     * the process until the no more messages at an earlier point in time have to be processed.
     *
     * @param duration The duration of simulation time to wait before resuming execution.
     * @param queue The mutable queue to push the messages to.
     */
    suspend fun hold(duration: Duration, queue: Queue<Any>)

    /**
     * Retrieve and remove a single message from the instance's mailbox, suspending the function if the mailbox is
     * empty. The execution is resumed after the head of the mailbox is removed and returned.
     *
     * @return The received message.
     */
    suspend fun receive(): Any

    /**
     * Retrieve and remove a single message from the instance's mailbox, suspending the function if the mailbox is
     * empty. The execution is either resumed after the head of the mailbox is removed and returned or when the timeout
     * has been reached.
     *
     * @return The received message or `null` if the timeout was reached.
     */
    suspend fun receive(timeout: Duration): Any?

    /**
     * Send the given message to the specified entity, without providing any guarantees about the actual delivery of
     * the message.
     *
     * @param msg The message to send.
     * @param delay The amount of time to wait before the message should be received by the entity.
     */
    suspend fun Entity<*, *>.send(msg: Any, delay: Duration = 0)

    /**
     * Send the given message to the specified entity, without providing any guarantees about the actual delivery of
     * the message.
     *
     * @param msg The message to send.
     * @param sender The sender of the message.
     * @param delay The amount of time to wait before the message should be received by the entity.
     */
    suspend fun Entity<*, *>.send(msg: Any, sender: Entity<*, *>, delay: Duration = 0)

    /**
     * This key provides users access to an untyped process context in case the coroutine runs inside a simulation.
     */
    companion object Key : CoroutineContext.Key<Context<*, *>>
}

/**
 * An [Interrupt] message is sent to an [Entity] process in order to interrupt its suspended state.
 *
 * @param reason The reason for the interruption of the process.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
open class Interrupt(reason: String) : Throwable(reason)
