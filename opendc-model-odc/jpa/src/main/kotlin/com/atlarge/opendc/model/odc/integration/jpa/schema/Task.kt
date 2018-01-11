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

package com.atlarge.opendc.model.odc.integration.jpa.schema

import com.atlarge.opendc.simulator.Instant
import com.atlarge.opendc.model.odc.platform.workload.Task
import com.atlarge.opendc.model.odc.platform.workload.TaskState
import javax.persistence.*

/**
 * A [Task] backed by the JPA API and an underlying database connection.
 *
 * @property id The unique identifier of the job.
 * @property flops The total amount of flops for the task.
 * @property dependency A dependency on another task.
 * @property parallelizable A flag to indicate the task is parallelizable.
 * @property startTime The start time in the simulation.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
@Entity
data class Task(
	override val id: Int,
	override val flops: Long,
	private val dependency: Task?,
	override val parallelizable: Boolean,
	val startTime: Instant
) : Task {
	/**
	 * The dependencies of the task.
	 */
	override lateinit var dependencies: Set<Task>
		private set

	/**
	 * The remaining flops for this task.
	 */
	override var remaining: Long = 0
		private set

	/**
	 * A flag to indicate whether the task has finished.
	 */
	override var finished: Boolean = false
		private set

	/**
	 * The state of the task.
	 */
	override lateinit var state: TaskState
		private set

	/**
	 * This method initialises the task object after it has been created by the JPA implementation. We use this
	 * initialisation method because JPA implementations only call the default constructor
	 */
	@PostLoad
	internal fun init() {
		remaining = flops
		dependencies = dependency?.let(::setOf) ?: emptySet()
		state = TaskState.Underway
	}

	/**
	 * This method is invoked when a task has arrived at a datacenter.
	 *
	 * @param time The moment in time the task has arrived at the datacenter.
	 */
	override fun arrive(time: Instant) {
		if (state !is TaskState.Underway) {
			throw IllegalStateException("The task has already been submitted to a datacenter")
		}
		remaining = flops
		state = TaskState.Queued(time)
	}

	/**
	 * Consume the given amount of flops of this task.
	 *
	 * @param time The current moment in time of the consumption.
	 * @param flops The total amount of flops to consume.
	 */
	override fun consume(time: Instant, flops: Long) {
		if (state is TaskState.Queued) {
			state = TaskState.Running(state as TaskState.Queued, time)
		} else if (finished) {
			return
		}
		remaining -= flops
		if (remaining <= 0) {
			remaining = 0
			finished = true
			state = TaskState.Finished(state as TaskState.Running, time)
		}
	}
}
