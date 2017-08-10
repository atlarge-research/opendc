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

package nl.atlarge.opendc.experiment.simulator

import nl.atlarge.opendc.topology.Entity

/**
 * A context for [Simulator] instance.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Context<E: Entity> {
	/**
	 * The current tick of the experiment.
	 */
	val tick: Long

	/**
	 * The [Entity] that is simulated.
	 */
	val entity: E

	/**
	 * Update the state of the entity being simulated.
	 *
	 * <p>Instead of directly mutating the entity, we create a new instance of the entity to prevent other objects
	 * referencing the old entity having their data changed.
	 *
	 * @param next The next state of the entity.
	 */
	fun update(next: E)

	/**
	 * Push the given given tick handler on the stack and change the simulator's behaviour to become the new tick
	 * handler.
	 *
	 * @param block The tick handler to push onto the stack.
	 */
	fun become(block: Context<E>.() -> Unit)

	/**
	 * Revert the behaviour of the simulator to the previous handler in the stack.
	 */
	fun unbecome()
}
