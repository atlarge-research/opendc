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

/**
 * A tick based clock which divides time into a discrete interval of points.
 *
 * @param initial The initial point in time of the clock.
 * @param tick The duration of a tick.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
class TickClock(initial: Instant = 0, override val tick: Duration = 1) : Clock {
	/**
	 * The moment in time the clock is currently at.
	 */
	override var now: Instant = initial
		private set

	/**
	 * Advance the clock to the given point in time.
	 *
	 * @param instant The moment in time to advance the clock to.
	 */
	override fun advanceTo(instant: Instant) {
		require(instant >= now) { "The point to advance to must be at the same point or further than now" }
		now = instant
	}

	/**
	 * Rewind the clock to the given point in time.
	 *
	 * @param instant The point in time to rewind the clock to.
	 */
	override fun rewindTo(instant: Instant) {
		require(now >= instant) { "The point to rewind to must be before the current point in time" }
		now = instant
	}
}
