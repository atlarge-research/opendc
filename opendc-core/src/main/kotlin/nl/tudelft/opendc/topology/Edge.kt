/*
 * MIT License
 *
 * Copyright (c) 2017 tudelft-atlarge
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

package nl.tudelft.opendc.topology

/**
 * An edge that represents a connection between exactly two instances of [Node].
 * Instances of [Edge] may be either directed or undirected.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Edge<out T> {
	/**
	 * The label of this edge.
	 */
	val label: T

	/**
	 * An [Edge] that is directed, having a source and destination [Node].
	 */
	interface Directed<out T>: Edge<T> {
		/**
		 * The source of the edge.
		 */
		val from: Node<*>

		/**
		 * The destination of the edge.
		 */
		val to: Node<*>
	}

	/**
	 * An [Edge] that is undirected.
	 */
	interface Undirected<out T>: Edge<T>

	/**
	 * Return the [Node] at the opposite end of this [Edge] from the
	 * specified node.
	 *
	 * Throws [IllegalArgumentException] if <code>node</code> is
	 * not incident to this edge.
	 *
	 * @param node The node to get the opposite of for this edge pair.
	 * @return The node at the opposite end of this edge from the specified node.
	 * @throws IllegalArgumentException if <code>node</code> is not incident to this edge.
	 */
	fun opposite(node: Node<*>): Node<*>

	/**
	 * Return a [Pair] representing this edge consisting of both incident nodes.
	 * Note that the pair is in no particular order.
	 *
	 * @return The edge represented as pair of both incident nodes.
	 */
	fun endpoints(): Pair<Node<*>, Node<*>>
}
