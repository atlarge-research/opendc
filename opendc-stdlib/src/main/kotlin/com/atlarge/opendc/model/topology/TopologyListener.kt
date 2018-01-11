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

package com.atlarge.opendc.model.topology;

import com.atlarge.opendc.simulator.Entity

/**
 * A listener interface for [Topology] instances. The methods of this interface are invoked on
 * mutation of the topology the listener is listening to.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface TopologyListener {
	/**
	 * This method is invoked when an [Entity] is added to the [Topology].
	 *
	 * @param node The entity that has been added to the [Topology].
	 */
	fun Topology.onNodeAdded(node: Entity<*, Topology>) {}

	/**
	 * This method is invoked when an [Entity] is removed from the [Topology].
	 *
	 * @param node The entity that has been removed from the [Topology].
	 */
	fun Topology.onNodeRemoved(node: Entity<*, Topology>) {}

	/**
	 * This method is invoked when an [Edge] is added to the [Topology].
	 *
	 * @param edge The edge that has been added to the [Topology].
	 */
	fun Topology.onEdgeAdded(edge: Edge<*>) {}

	/**
	 * This method is invoked when an [Edge] is removed from the [Topology].
	 *
	 * @param edge The entity that has been removed from the [Topology].
	 */
	fun Topology.onEdgeRemoved(edge: Edge<*>) {}
}
