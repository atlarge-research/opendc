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

import nl.atlarge.opendc.kernel.messaging.Receipt
import nl.atlarge.opendc.kernel.time.Clock
import nl.atlarge.opendc.kernel.time.Duration
import nl.atlarge.opendc.kernel.time.Instant
import nl.atlarge.opendc.topology.Entity
import nl.atlarge.opendc.topology.Topology
import java.lang.Process

/**
 * A message based discrete event simulation facilitated by a simulation [Kernel].
 *
 * In order for the simulation to run, the simulation kernel needs to bootstrapped by an set of messages to be processed
 * initially by entities in the topology of the simulation. Otherwise, the simulation will immediately exit.
 * Bootstrapping can be achieved by scheduling messages before running the simulation via [Simulation.schedule]:
 *
 * `val simulation = kernel.create(topology).apply {
 *		schedule(Boot, entity)
 * }`
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Simulation {
	/**
	 * The [Kernel] that facilitates the simulation.
	 */
	val kernel: Kernel

	/**
	 * The [Topology] over which the simulation is run.
	 */
	val topology: Topology

	/**
	 * The [Clock] instance that keeps track of simulation time.
	 */
	val clock: Clock

	/**
	 * The observable state of an [Entity] in simulation, which is provided by the simulation context.
	 */
	val <E : Entity<S>, S> E.state: S

	/**
	 * Step through one cycle in the simulation. This method will process all events in a single tick, update the
	 * internal clock and then return the control to the user.
	 */
	fun step()

	/**
	 * Run a simulation over the specified [Topology].
	 * This method will step through multiple cycles in the simulation until no more message exist in the queue.
	 */
	fun run()

	/**
	 * Run a simulation over the specified [Topology], stepping through cycles until the specified clock tick has
	 * occurred. The control is then handed back to the user.
	 *
	 * @param until The point in simulation time at which the simulation should be paused and the control is handed
	 * 				back to the user.
	 */
	fun run(until: Instant)

	/**
	 * Schedule a message for processing by a [Process].
	 *
	 * @param message The message to schedule.
	 * @param destination The destination of the message.
	 * @param sender The sender of the message.
	 * @param delay The amount of time to wait before processing the message.
	 * @return A [Receipt] of the message that has been scheduled.
	 */
	fun schedule(message: Any, destination: Entity<*>, sender: Entity<*>? = null, delay: Duration = 0): Receipt
}
