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

package nl.atlarge.opendc.experiment.simulator

import nl.atlarge.opendc.experiment.messaging.Port
import nl.atlarge.opendc.topology.Entity

/**
 * A simulator that simulates a single entity in the topology of a cloud network.
 *
 * @param ctx The context in which the simulation is run.
 * @param <E> The type of entity to simulate.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
abstract class AbstractSimulator<E: Entity>(val ctx: Context<E>): Simulator<E> {
	/**
	 * The [Entity] that is simulated.
	 */
	val self: E = ctx.entity

	/**
	 * Create a [Port] of the given type.
	 *
	 * @param name The name of the label to create the port for.
	 * @return The port that has been created or the cached result.
	 */
	inline fun <reified E: Entity, T> port(name: String): Port<E, T> {
		throw NotImplementedError()
	}
}
