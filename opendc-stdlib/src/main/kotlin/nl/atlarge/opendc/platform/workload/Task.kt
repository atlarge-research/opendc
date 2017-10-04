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

package nl.atlarge.opendc.platform.workload

import nl.atlarge.opendc.kernel.time.Instant
import nl.atlarge.opendc.topology.machine.Machine

/**
 * A task that runs as part of a [Job] on a [Machine].
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Task {
	/**
	 * The unique identifier of the task.
	 */
	val id: Int

	/**
	 * The amount of flops for this task.
	 */
	val flops: Long

	/**
	 * The dependencies of the task.
	 */
	val dependencies: Set<Task>

	/**
	 * A flag to indicate the task is parallelizable.
	 */
	val parallelizable: Boolean

	/**
	 * The remaining flops for this task.
	 */
	val remaining: Long

	/**
	 * The state of the task.
	 */
	val state: TaskState

	/**
	 * A flag to indicate whether the task is ready to be started.
	 */
	val ready: Boolean
		get() = !dependencies.any { !it.finished }

	/**
	 * A flag to indicate whether the task has finished.
	 */
	val finished: Boolean
		get() = state is TaskState.Finished

	/**
	 * This method is invoked when a task has arrived at a datacenter.
	 *
	 * @param time The moment in time the task has arrived at the datacenter.
	 */
	fun arrive(time: Instant)

	/**
	 * Consume the given amount of flops of this task.
	 *
	 * @param time The current moment in time of the consumption.
	 * @param flops The total amount of flops to consume.
	 */
	fun consume(time: Instant, flops: Long)
}
