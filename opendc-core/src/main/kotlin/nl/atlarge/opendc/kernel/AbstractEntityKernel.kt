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

import nl.atlarge.opendc.kernel.messaging.ReadablePort
import nl.atlarge.opendc.kernel.messaging.WritableChannel
import nl.atlarge.opendc.topology.Edge
import nl.atlarge.opendc.topology.Entity
import nl.atlarge.opendc.topology.Node

/**
 * A simulation kernel that simulates a single entity in the topology of a cloud network.
 *
 * @param ctx The context in which the simulation is run.
 * @param E The shape of the component to simulate.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
abstract class AbstractEntityKernel<E: Entity<*>>(private val ctx: EntityContext<E>): Kernel<EntityContext<E>> {
	/**
	 * The [Node] that is simulated.
	 */
	val self: Node<E> = ctx.component

	/**
	 * Create a [WritableChannel] over the edge with the given tag.
	 *
	 * @param tag The tag of the edge to create a channel over.
	 * @return The channel that has been created or the cached result.
	 */
	inline fun <reified T: Edge<*>> output(tag: String): WritableChannel<T> {
		TODO()
	}

	/**
	 * Create a [ReadablePort] over the edges with the given tag.
	 *
	 * @param tag The tag of the edges to create a port over.
	 * @return The port that has been created or the cached result.
	 */
	inline fun <reified T: Edge<*>> input(tag: String): ReadablePort<T> {
		TODO()
	}
}
