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

package com.atlarge.opendc.model.topology

import com.atlarge.opendc.simulator.Entity

/**
 * A subinterface of [Topology] which adds mutation methods. When mutation is not required, users
 * should prefer the [Topology] interface.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface MutableTopology : Topology, MutableSet<Entity<*, Topology>> {
	/**
	 * Create a directed, labeled edge between two nodes in the topology.
	 *
	 * @param from The source of the edge.
	 * @param to The destination of the edge.
	 * @param label The label of the edge.
	 * @param tag The tag of the edge that uniquely identifies the relationship the edge represents.
	 * @return The edge that has been created.
	 */
	fun <T> connect(from: Entity<*, Topology>, to: Entity<*, Topology>, label: T, tag: String? = null): Edge<T>

	/**
	 * Create a directed, unlabeled edge between two nodes in the topology.
	 *
	 * @param from The source of the edge.
	 * @param to The destination of the edge.
	 * @param tag The tag of the edge that uniquely identifies the relationship the edge represents.
	 * @return The edge that has been created.
	 */
	fun connect(from: Entity<*, Topology>, to: Entity<*, Topology>, tag: String? = null): Edge<Unit> =
		connect(from, to, Unit, tag)

	/**
	 * Create a directed, unlabeled edge between two nodes in the topology.
	 *
	 * @param dest The destination of the edge.
	 * @return The edge that has been created.
	 */
	infix fun Entity<*, Topology>.to(dest: Entity<*, Topology>): Edge<Unit> = connect(this, dest)
}
