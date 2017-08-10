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
 * A simulator that simulates a single [Entity] instance in a cloud network.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Simulator<E: Entity> {
	/**
	 * This method is invoked once at the start of the simulation to setup the initial state of the [Simulator].
	 */
	fun Context<E>.setUp() {}

	/**
	 * This method is invoked at least once before a tick. This allows a [Simulator] to setup its state before a tick
	 * event.
	 *
	 * <p>The pre-tick consists of multiple sub-cycles in which all messages which have been sent
	 * in the previous sub-cycle can optionally be processed in the sub-cycle by the receiving [Simulator].
	 */
	fun Context<E>.preTick() {}

	/**
	 * This method is invoked once per tick, which allows a [Simulator] to process events to simulate an entity in a
	 * cloud network.
	 */
	fun Context<E>.tick() {}

	/**
	 * This method is invoked at least once per tick. This allows the [Simulator] to do work after a tick.
	 *
	 * <p>Like the pre-tick, the post-tick consists of multiple sub-cycles in which all messages which have been sent
	 * in the previous sub-cycle can optionally be processed in the sub-cycle by the receiving [Simulator].
	 */
	fun Context<E>.postTick() {}

	/**
	 * This method is invoked once at the end of the simulation to tear down resources of the [Simulator].
	 */
	fun Context<E>.tearDown() {}
}
