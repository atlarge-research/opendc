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

package nl.atlarge.opendc.kernel.time

import nl.atlarge.opendc.kernel.Simulation

/**
 * A clock controls and provides access to the simulation time of a [Simulation].
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Clock {
	/**
	 * The moment in time the clock is currently at.
	 */
	val now: Instant

	/**
	 * The duration of a tick in this clock. This is an arbitrary duration of time in which entities in simulation
	 * perform some defined amount of work.
	 */
	val tick: Duration

	/**
	 * Advance the clock by the given duration.
	 *
	 * @param duration The duration to advance the clock by.
	 */
	fun advance(duration: Duration) {
		require(duration >= 0) { "The duration to advance the clock must not be a negative number" }
		advanceTo(now + duration)
	}

	/**
	 * Rewind the clock by the given duration.
	 *
	 * @param duration The duration to rewind the clock by.
	 */
	fun rewind(duration: Duration) {
		require(duration >= 0) { "The duration to rewind the clock must not be a negative number" }
		rewindTo(now - duration)
	}

	/**
	 * Rewind the clock to the given point in time.
	 *
	 * @param instant The point in time to rewind the clock to.
	 */
	fun rewindTo(instant: Instant)

	/**
	 * Advance the clock to the given point in time.
	 *
	 * @param instant The point in time to advance the clock to.
	 */
	fun advanceTo(instant: Instant)
}
