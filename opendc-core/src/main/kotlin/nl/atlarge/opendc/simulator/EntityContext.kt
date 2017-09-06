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

package nl.atlarge.opendc.simulator

import nl.atlarge.opendc.topology.Entity
import nl.atlarge.opendc.topology.Node

/**
 * The context provided to a simulation kernel for stateful entities in the topology.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface EntityContext<out T: Entity<*>>: Context<Node<T>> {
	/**
	 * Update the state of the entity being simulated.
	 *
	 * <p>Instead of directly mutating the entity, we create a new instance of the entity to prevent other objects
	 * referencing the old entity having their data changed.
	 *
	 * @param next The next state of the entity.
	 */
	suspend fun <C: EntityContext<E>, E: Entity<S>, S> C.update(next: S)
}
