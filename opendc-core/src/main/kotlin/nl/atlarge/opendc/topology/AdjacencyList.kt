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

import nl.atlarge.opendc.topology.Edge as BaseEdge
import java.util.concurrent.atomic.AtomicInteger

/**
 * This module provides a [Topology] implementation backed internally by an adjacency list.
 *
 * This implementation is best suited for sparse graphs, where an adjacency matrix would take up too much space with
 * empty cells.
 *
 * *Note that this implementation is not synchronized.*
 */
object AdjacencyList {
	/**
	 * Return a [TopologyBuilder] that constructs the topology represents as an adjacency list.
	 *
	 * @return A [TopologyBuilder] instance.
	 */
	fun builder(): TopologyBuilder = AdjacencyListTopologyBuilder()
}

/**
 * A builder for [Topology] instances, which is backed by an adjacency list.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
internal class AdjacencyListTopologyBuilder : TopologyBuilder {
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
internal class AdjacencyListTopology : MutableTopology {
	/**
	 * The identifier for the next node in the graph.
	 */
	private var nextId: AtomicInteger = AtomicInteger(0)

	/**
	 * A mapping of nodes to their internal representation with the edges of the nodes.
	 */
	private var nodes: MutableMap<Entity<*>, Node> = HashMap()

	// Topology

	/**
	 * The listeners of this topology.
	 */
	override val listeners: MutableSet<TopologyListener> = HashSet()

	/**
	 * A unique identifier of this node within the topology.
	 */
	override val Entity<*>.id: Int
		get() = nodes[this]!!.id

	/**
	 * The set of ingoing edges of this node.
	 */
	override val Entity<*>.ingoingEdges: MutableSet<BaseEdge<*>>
		get() = nodes[this]!!.ingoingEdges

	/**
	 * The set of outgoing edges of this node.
	 */
	override val Entity<*>.outgoingEdges: MutableSet<BaseEdge<*>>
		get() = nodes[this]!!.outgoingEdges

	// MutableTopology

	/**
	 * Create a directed edge between two [Node]s in the topology.
	 *
	 * @param from The source of the edge.
	 * @param to The destination of the edge.
	 * @param label The label of the edge.
	 * @param tag The tag of the edge that uniquely identifies the relationship the edge represents.
	 * @return The edge that has been created.
	 */
	override fun <T> connect(from: Entity<*>, to: Entity<*>, label: T, tag: String?): BaseEdge<T> {
		if (!contains(from) || !contains(to))
			throw IllegalArgumentException("One of the entities is not part of the topology")
		val edge = Edge(label, tag, from, to)
		from.outgoingEdges.add(edge)
		to.ingoingEdges.add(edge)
		listeners.forEach { it.run { this@AdjacencyListTopology.onEdgeAdded(edge) } }
		return edge
	}

	// Cloneable

	/**
	 * Create a copy of the graph.
	 *
	 * @return A new [Topology] instance with a copy of the graph.
	 */
	override public fun clone(): Topology {
		val copy = AdjacencyListTopology()
		copy.nextId = AtomicInteger(nextId.get())
		copy.nodes = HashMap(nodes)
		return copy
	}

	// Set

	/**
	 * Returns the size of the collection.
	 */
	override val size: Int = nodes.size

	/**
	 * Checks if the specified element is contained in this collection.
	 */
	override fun contains(element: Entity<*>): Boolean = nodes.contains(element)

	/**
	 * Checks if all elements in the specified collection are contained in this collection.
	 */
	override fun containsAll(elements: Collection<Entity<*>>): Boolean = elements.all { nodes.containsKey(it) }

	/**
	 * Returns `true` if the collection is empty (contains no elements), `false` otherwise.
	 */
	override fun isEmpty(): Boolean = nodes.isEmpty()

	// MutableSet

	/**
	 * Add a node to the graph.
	 *
	 * @param element The element to add to this graph.
	 * @return `true` if the graph has changed, `false` otherwise.
	 */
	override fun add(element: Entity<*>): Boolean {
		if (nodes.putIfAbsent(element, Node(nextId.getAndIncrement())) == null) {
			listeners.forEach { it.run { this@AdjacencyListTopology.onNodeAdded(element) } }
			return true
		}
		return false
	}

	/**
	 * Add all nodes in the specified collection to the graph.
	 *
	 * @param elements The nodes to add to this graph.
	 * @return `true` if the graph has changed, `false` otherwise.
	 */
	override fun addAll(elements: Collection<Entity<*>>): Boolean = elements.any { add(it) }

	/**
	 * Remove all nodes and their respective edges from the graph.
	 */
	override fun clear() = nodes.clear()

	/**
	 * Remove the given node and its edges from the graph.
	 *
	 * @param element The element to remove from the graph.
	 * @return `true` if the graph has changed, `false` otherwise.
	 */
	override fun remove(element: Entity<*>): Boolean {
		nodes[element]?.ingoingEdges?.forEach {
			it.from.outgoingEdges.remove(it)
		}
		nodes[element]?.outgoingEdges?.forEach {
			it.to.ingoingEdges.remove(it)
		}
		if (nodes.keys.remove(element)) {
			listeners.forEach { it.run { this@AdjacencyListTopology.onNodeRemoved(element) } }
			return true
		}
		return false
	}


	/**
	 * Remove all nodes in the given collection from the graph.
	 *
	 * @param elements The elements to remove from the graph.
	 * @return `true` if the graph has changed, `false` otherwise.
	 */
	override fun removeAll(elements: Collection<Entity<*>>): Boolean = elements.any(this::remove)

	/**
	 * Remove all nodes in the graph, except those in the specified collection.
	 *
	 * Take note that this method currently only guarantees a maximum runtime complexity of O(n^2).
	 *
	 * @param elements The elements to retain in the graph.
	 */
	override fun retainAll(elements: Collection<Entity<*>>): Boolean {
		val iterator = nodes.keys.iterator()
		var changed = false
		while (iterator.hasNext()) {
			val entity = iterator.next()

			if (entity !in elements) {
				iterator.remove()
				changed = true
			}
		}
		return changed
	}

	/**
	 * Return a mutable iterator over the nodes of the graph.
	 *
	 * @return A [MutableIterator] over the nodes of the graph.
	 */
	override fun iterator(): MutableIterator<Entity<*>> = nodes.keys.iterator()

	/**
	 * The internal representation of a node within the graph.
	 */
	internal data class Node(val id: Int) {
		val ingoingEdges: MutableSet<BaseEdge<*>> = HashSet()
		val outgoingEdges: MutableSet<BaseEdge<*>> = HashSet()
	}

	/**
	 * The internal representation of an edge within the graph.
	 */
	internal class Edge<out T>(override val label: T,
							   override val tag: String?,
							   override val from: Entity<*>,
							   override val to: Entity<*>) : BaseEdge<T>
}
