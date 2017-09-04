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

package nl.atlarge.opendc.topology

/**
 * A labeled node of graph representing an entity in a specific logical topology of a cloud network.
 *
 * <p>A [Node] is instantiated and managed by a [Topology] instance containing user-specified data in its label.
 *
 * @param T The entity type the node represents in a logical topology.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Node<out T: Entity<*>>: Component<T> {
	/**
	 * A unique identifier of this node within the topology.
	 */
	val id: Int

	/**
	 * Return the set of ingoing edges of this node.
	 *
	 * @return All edges whose destination is this node.
	 */
	fun ingoingEdges(): Set<Edge<*>>

	/**
	 * Return the set of outgoing edges of this node.
	 *
	 * @return  All edges whose source is this node.
	 */
	fun outgoingEdges(): Set<Edge<*>>

	/**
	 * The [Entity] this node represents within a logical topology of a cloud network.
	 */
	val entity: T
		get() = label
}

/**
 * Return the set of ingoing edges of this node with the given tag.
 *
 * @param tag The tag of the edges to get.
 * @param T The shape of the label of these edges.
 * @return All edges whose destination is this node and have the given tag.
 */
inline fun <reified T> Node<*>.ingoing(tag: String) =
	ingoingEdges().filter { it.tag == tag }.map { it as T }.toSet()


/**
 * Return the set of outgoing edges of this node with the given tag.
 *
 * @param tag The tag of the edges to get.
 * @param T The shape of the label of these edges.
 * @return All edges whose source is this node and have the given tag.
 */
inline fun <reified T> Node<*>.outgoing(tag: String) =
	outgoingEdges().filter { it.tag == tag }.map { it as T }.toSet()
