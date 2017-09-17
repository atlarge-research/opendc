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
import nl.atlarge.opendc.topology.Entity
import nl.atlarge.opendc.topology.Topology
import nl.atlarge.opendc.topology.TopologyContext

/**
 * This interface provides a context for simulation [Process]es, which defines the environment in which the simulation
 * is run and provides means of communicating with other entities in the environment and control its own behaviour.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Context<out E : Entity<*>> : Readable, Writable, TopologyContext {
	/**
	 * The [Topology] over which the simulation is run.
	 */
	val topology: Topology

	/**
	 * The global [Clock] that keeps track of the simulation time.
	 */
	val clock: Clock

	/**
	 * The [Entity] in simulation by the [Process].
	 */
	val entity: E

	/**
	 * The observable state of an [Entity] in simulation, which is provided by the simulation context.
	 */
	val <E : Entity<S>, S> E.state: S

	/**
	 * Interrupt the [Process] of an [Entity] in simulation.
	 */
	suspend fun Entity<*>.interrupt()

	/**
	 * Suspend the [Process] of the [Entity] in simulation until the next tick has occurred in the simulation.
	 */
	suspend fun tick(): Boolean

	/**
	 * Suspend the [Process] of the [Entity] in simulation for <code>n</code> ticks before resuming execution.
	 *
	 * @param n The amount of ticks to suspend the process.
	 */
	suspend fun wait(n: Int)

	/**
	 * Update the observable state of the entity being simulated.
	 *
	 * Instead of directly mutating the entity, we create a new instance of the entity to prevent other objects
	 * referencing the old entity having their data changed.
	 *
	 * @param next The next state of the entity.
	 */
	suspend fun <C : Context<E>, E : Entity<S>, S> C.update(next: S)
}
