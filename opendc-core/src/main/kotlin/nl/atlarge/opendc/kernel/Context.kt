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

package nl.atlarge.opendc.kernel

import nl.atlarge.opendc.kernel.messaging.Readable
import nl.atlarge.opendc.kernel.messaging.Writable
import nl.atlarge.opendc.kernel.time.Clock
import nl.atlarge.opendc.kernel.time.Duration
import nl.atlarge.opendc.kernel.time.Instant
import nl.atlarge.opendc.topology.Entity
import nl.atlarge.opendc.topology.Topology
import nl.atlarge.opendc.topology.TopologyContext
import java.lang.Process
import java.util.*

/**
 * This interface provides a context for simulation [Process]es, which defines the environment in which the simulation
 * is run and provides means of communicating with other entities in the environment and control its own behaviour.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Context<out E : Entity<*>> : Readable, Writable, TopologyContext {
	/**
	 * The [Entity] in simulation by the [Process].
	 */
	val entity: E

	/**
	 * The [Topology] over which the simulation is run.
	 */
	val topology: Topology

	/**
	 * The current point in simulation time.
	 */
	val time: Instant

	/**
	 * The duration between the current point in simulation time and the last point in simulation time where the
	 * [Process] has executed some work. This means the `run()` co-routine has been resumed.
	 */
	val delta: Duration

	/**
	 * The observable state of an [Entity] in simulation, which is provided by the simulation context.
	 */
	val <E : Entity<S>, S> E.state: S

	/**
	 * Update the observable state of the entity being simulated.
	 *
	 * Instead of directly mutating the entity, we create a new instance of the entity to prevent other objects
	 * referencing the old entity having their data changed.
	 *
	 * @param next The next state of the entity.
	 */
	suspend fun <C : Context<E>, E : Entity<S>, S> C.update(next: S)

	/**
	 * Interrupt the [Process] of an [Entity] in simulation.
	 *
	 * If a [Process] has been suspended, the suspending call will throw an [Interrupt] object as a result of this call.
	 * Make sure the [Process] actually has error handling in place, so it won't take down the whole [Process].
	 */
	suspend fun Entity<*>.interrupt()

	/**
	 * Suspend the [Process] of the [Entity] in simulation for the given duration of simulation time before resuming
	 * execution and drop all messages that are received during this period.
	 *
	 * A call to this method will not make the [Process] sleep for the actual duration of time, but instead suspend
	 * the process until the no more messages at an earlier point in time have to be processed.
	 *
	 * @param duration The duration of simulation time to wait before resuming execution.
	 */
	suspend fun wait(duration: Duration)

	/**
	 * Suspend the [Process] of the [Entity] in simulation for the given duration of simulation time before resuming
	 * execution and push all messages that are received during this period to the given queue.
	 *
	 * A call to this method will not make the [Process] sleep for the actual duration of time, but instead suspend
	 * the process until the no more messages at an earlier point in time have to be processed.
	 *
	 * @param duration The duration of simulation time to wait before resuming execution.
	 * @param queue The mutable queue to push the messages to.
	 */
	suspend fun wait(duration: Duration, queue: Queue<Any>)

	/**
	 * Suspend the [Process] of the [Entity] in simulation for one tick in simulation time which is defined by the
	 * [Clock].
	 *
	 * @return `true` to allow usage in while statements.
	 */
	suspend fun tick(): Boolean
}
