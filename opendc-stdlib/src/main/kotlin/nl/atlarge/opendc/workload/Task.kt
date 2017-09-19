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

package nl.atlarge.opendc.workload

/**
 * A task represents some computation that is part of a [Job].
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
data class Task(
	val id: Int,
	val dependencies: Set<Task>,
	val flops: Long
) {
	/**
	 * The remaining amount of flops to compute.
	 */
	var remaining: Long = flops
		private set

	/**
	 * A flag to indicate whether the task is finished.
	 */
	var finished: Boolean = false
		private set

	/**
	 * Determine whether the task is ready to be processed.
	 *
	 * @return `true` if the task is ready to be processed, `false` otherwise.
	 */
	fun isReady() = dependencies.all { it.finished }

	/**
	 * Consume the given amount of flops of this task.
	 *
	 * @param flops The total amount of flops to consume.
	 */
	fun consume(flops: Long) {
		if (finished)
			return
		if (remaining <= flops) {
			finished = true
			remaining = 0
		} else {
			remaining -= flops
		}
	}
}
