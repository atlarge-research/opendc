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

import nl.atlarge.opendc.kernel.Context
import nl.atlarge.opendc.topology.container.Datacenter
import nl.atlarge.opendc.topology.machine.Machine

/**
 * A task represents some computation that is part of a [Job].
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
	 * The remaining amount of flops to compute.
	 */
	val remaining: Long

	/**
	 * A flag to indicate the task has been accepted by the datacenter.
	 */
	val accepted: Boolean

	/**
	 * A flag to indicate the task has been started.
	 */
	val started: Boolean

	/**
	 * A flag to indicate whether the task is finished.
	 */
	val finished: Boolean

	/**
	 * Determine whether the task is ready to be processed.
	 *
	 * @return `true` if the task is ready to be processed, `false` otherwise.
	 */
	fun isReady() = dependencies.all { it.finished }

	/**
	 * Accept the task into the scheduling queue.
	 */
	fun Context<Datacenter>.accept()

	/**
	 * Start a task.
	 */
	fun Context<Machine>.start()

	/**
	 * Consume the given amount of flops of this task.
	 *
	 * @param flops The total amount of flops to consume.
	 */
	fun Context<Machine>.consume(flops: Long)

	/**
	 * Finalise the task.
	 */
	fun Context<Machine>.finalize()
}
