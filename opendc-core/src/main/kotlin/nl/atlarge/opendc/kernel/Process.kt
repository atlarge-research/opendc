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

/**
 * A [Process] defines the behaviour of an [Entity] within simulation.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Process<in E : Entity<*>> {
	/**
	 * This method is invoked to start the simulation an [Entity] associated with this [Process].
	 *
	 * This method is assumed to be running during a simulation, but should hand back control to the simulator at
	 * some point by suspending the process. This allows other processes to do work in the current tick of the
	 * simulation.
	 * Suspending the process can be achieved by calling suspending method in the context:
	 * 	- [Context.tick]	- Wait for the next tick to occur
	 * 	- [Context.wait]	- Wait for `n` amount of ticks before resuming execution.
	 * 	- [Context.receive]	- Wait for a message to be received in the mailbox of the [Entity] before resuming
	 * 	execution.
	 *
	 * If this method exits early, before the simulation has finished, the entity is assumed to be shutdown and its
	 * simulation will not run any further.
	 */
	suspend fun Context<E>.run()
}
