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

import nl.atlarge.opendc.topology.Entity
import nl.atlarge.opendc.topology.Topology
import java.lang.Process

/**
 * A message-based discrete event simulator (DES). This interface allows running simulations over a [Topology].
 * This discrete event simulator works by having entities in a [Topology] interchange messages between each other and
 * updating their observable state accordingly.
 *
 * In order to run a simulation, a kernel needs to bootstrapped by an initial set of messages to be processed by
 * entities in the topology of the simulation. Otherwise, the simulation will immediately exit.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Kernel {
	/**
	 * The [Topology] over which the simulation is run.
	 */
	val topology: Topology

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
	 * Schedule a message for processing by a [Process].
	 *
	 * @param message The message to schedule.
	 * @param destination The destination of the message.
	 * @param sender The sender of the message.
	 * @param delay The amount of ticks to wait before processing the message.
	 */
	fun schedule(message: Any?, destination: Entity<*>, sender: Entity<*>? = null, delay: Int = 0)
}
