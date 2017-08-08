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

/**
 * A simulator that simulates a single entity in the topology of a cloud network.
 *
 * @param entity The entity to simulate.
 * @param ctx The context in which the simulation is run.
 * @param <E> The type of entity to simulate.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
abstract class Simulator<out E: Entity>(val entity: E, val ctx: SimulatorContext) {
	/**
	 * This method is invoked at least once before a tick. This allows the [Simulator] to setup its state before a tick
	 * event.
	 *
	 * <p>The pre-tick consists of multiple sub-cycles in which all messages which have been sent
	 * in the previous sub-cycle can optionally be processed in the sub-cycle by the receiving [Simulator].
	 */
	fun preTick() {}

	/**
	 * This method is invoked once per tick, which allows the [Simulator] to process events and simulate an entity in a
	 * cloud network.
	 */
	fun tick() {}

	/**
	 * This method is invoked at least once per tick. This allows the [Simulator] to do work after a tick.
	 *
	 * <p>Like the pre-tick, the post-tick consists of multiple sub-cycles in which all messages which have been sent
	 * in the previous sub-cycle can optionally be processed in the sub-cycle by the receiving [Simulator].
	 */
	fun postTick() {}

	/**
	 * Send the given message to the given [Entity] for processing.
	 *
	 * @param destination The entity to send the message to.
	 * @param message The message to send to the entity.
	 */
	fun send(destination: Entity, message: Any?) {}
}
