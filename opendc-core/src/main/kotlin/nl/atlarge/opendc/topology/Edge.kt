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

import java.util.*

/**
 * An undirected edge that represents a connection between exactly two instances of [Entity].
 *
 * @param from The first incident node.
 * @param to The second incident node.
 * @param label The label of the edge.
 * @param <T> The data type of the label value.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
class Edge<out T>(val from: Entity, val to: Entity, val label: Label<T>) {
	/**
	 * Return the [Entity] at the opposite end of this [Edge] from the
	 * specified entity.
	 *
	 * Throws [IllegalArgumentException] if <code>entity</code> is
	 * not incident to this edge.
	 *
	 * @param entity The entity to get the opposite of for this edge pair.
	 * @return The entity at the opposite end of this edge from the specified entity.
	 * @throws IllegalArgumentException if <code>entity</code> is not incident to this edge.
	 */
	fun opposite(entity: Entity): Entity = when (entity) {
		from -> to
		to -> from
		else -> throw IllegalArgumentException()
	}

	/**
	 * Return a [Pair] representing this edge consisting of both incident nodes.
	 * Note that the pair is in no particular order.
	 *
	 * @return The edge represented as pair of both incident nodes.
	 */
	fun endpoints(): Pair<Entity, Entity> = Pair(from, to)

	/**
	 * Determine whether the given object is equal to this instance.
	 *
	 * @param other The other object to compare against.
	 * @return <code>true</code> both edges are equal, <code>false</code> otherwise.
	 */
	override fun equals(other: Any?): Boolean =
		if (other is Edge<*>) {
			from == other.from && to == other.to ||
				from == other.to && to == other.from
		} else {
			false
		}

	/**
	 * Return the hash code of this edge pair.
	 *
	 * @return The hash code of this edge pair.
	 */
	override fun hashCode(): Int {
		return Objects.hash(from, to)
	}

	/**
	 * Return a string representation of this [Edge].
	 *
	 * @return A string representation of this [Edge].
	 */
	override fun toString(): String {
		return "Edge($from<->$to)"
	}
}
