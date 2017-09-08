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

import java.util.concurrent.atomic.AtomicInteger

/**
 * A builder for [Topology] instances, which is backed by an adjacency list.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
class AdjacencyListTopologyBuilder: TopologyBuilder {
	/**
	 * Build a [Topology] instance from the current state of this builder.
	 *
	 * @return The graph built from this builder.
	 */
	override fun build(): MutableTopology = AdjacencyListTopology()
}

/**
 * A [Topology] whose graph is represented as adjacency list.
 */
internal class AdjacencyListTopology: MutableTopology {
	private val nextId: AtomicInteger = AtomicInteger(0)
	private val nodes: MutableList<Node<*>> = ArrayList()

	/**
	 * Returns the size of the collection.
	 */
	override val size: Int = nodes.size

	/**
	 * Checks if the specified element is contained in this collection.
	 */
	override fun contains(element: Node<*>): Boolean = nodes.contains(element)

	/**
	 * Checks if all elements in the specified collection are contained in this collection.
	 */
	override fun containsAll(elements: Collection<Node<*>>): Boolean = nodes.containsAll(elements)

	/**
	 * Returns `true` if the collection is empty (contains no elements), `false` otherwise.
	 */
	override fun isEmpty(): Boolean = nodes.isEmpty()

	/**
	 * Create a [Node] in this [Topology] for the given [Entity].
	 *
	 * @param entity The entity to create a node for.
	 * @return The node created for the given entity.
	 */
	override fun <T : Entity<*>> node(entity: T): Node<T> {
		val node = AdjacencyListNode(nextId.getAndIncrement(), entity)
		nodes.add(node)
		return node
	}

	/**
	 * Create a directed edge between two [Node]s in the topology.
	 *
	 * @param from The source of the edge.
	 * @param to The destination of the edge.
	 * @param label The label of the edge.
	 * @param tag The tag of the edge that uniquely identifies the relationship the edge represents.
	 * @return The edge that has been created.
	 */
	override fun <T> connect(from: Node<*>, to: Node<*>, label: T, tag: String?): Edge<T> {
		if (from !is AdjacencyListNode<*> || to !is AdjacencyListNode<*>)
			throw IllegalArgumentException()
		if (!from.validate(this) || !to.validate(this))
			throw IllegalArgumentException()
		val edge: Edge<T> = AdjacencyListEdge(label, tag, from, to)
		from.outgoingEdges.add(edge)
		to.ingoingEdges.add(edge)
		return edge
	}

	/**
	 * Returns an iterator over the elements of this object.
	 */
	override fun iterator(): MutableIterator<Node<*>> = nodes.iterator()

	internal inner class AdjacencyListNode<out T: Entity<*>>(override val id: Int, override val label: T): Node<T> {
		override var ingoingEdges: MutableSet<Edge<*>> = HashSet()
		override var outgoingEdges: MutableSet<Edge<*>> = HashSet()
		override fun toString(): String = label.toString()
		internal fun validate(instance: AdjacencyListTopology) = this@AdjacencyListTopology == instance
	}

	internal class AdjacencyListEdge<out T>(override val label: T,
											override val tag: String?,
											override val from: Node<*>,
											override val to: Node<*>): Edge<T> {
		override fun toString(): String = label.toString()
	}
}
