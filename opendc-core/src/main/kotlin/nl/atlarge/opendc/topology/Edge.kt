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
 * An edge that represents a directed relationship between exactly two [Node]s in a logical topology of a cloud network.
 *
 * @param T The relationship type the edge represents within a logical topology.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Edge<out T>: Component<T> {
	/**
	 * A tag to uniquely identify the relationship this edge represents.
	 */
	val tag: String?

	/**
	 * The source of the edge.
	 *
	 * This property is not guaranteed to have a runtime complexity of <code>O(1)</code>, but must be at least
	 * <code>O(n)</code>, with respect to the size of the topology.
	 */
	val from: Node<*>

	/**
	 * The destination of the edge.
	 *
	 * This property is not guaranteed to have a runtime complexity of <code>O(1)</code>, but must be at least
	 * <code>O(n)</code>, with respect to the size of the topology.
	 */
	val to: Node<*>
}
