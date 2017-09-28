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

package nl.atlarge.opendc.integration.jpa.schema

import nl.atlarge.opendc.kernel.Context
import nl.atlarge.opendc.kernel.time.Instant
import nl.atlarge.opendc.platform.workload.Task
import nl.atlarge.opendc.topology.container.Datacenter
import nl.atlarge.opendc.topology.machine.Machine
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
	override val dependencies: Set<Task>
		get() {
			if (_dependencies != null)
				return _dependencies!!
			_dependencies = dependency?.let(::setOf) ?: emptySet()
			return _dependencies!!
		}

	/**
	 * The dependencies set cache.
	 */
	private var _dependencies: Set<Task>? = null

	/**
	 * The remaining amount of flops to compute.
	 */
	override var remaining: Long
		get() {
			if (_remaining == null)
				_remaining = flops
			return _remaining!!
		}
		private set(value) { _remaining = value }
	private var _remaining: Long? = -1

	/**
	 * A flag to indicate the task has been accepted by the datacenter.
	 */
	override var accepted: Boolean = false
		private set

	/**
	 * A flag to indicate the task has been started.
	 */
	override var started: Boolean = false
		private set

	/**
	 * A flag to indicate whether the task is finished.
	 */
	override var finished: Boolean = false
		private set

	/**
	 * Accept the task into the scheduling queue.
	 */
	override fun Context<Datacenter>.accept() {
		accepted = true
	}

	/**
	 * Start a task.
	 */
	override fun Context<Machine>.start() {
		started = true
	}

	/**
	 * Consume the given amount of flops of this task.
	 *
	 * @param flops The total amount of flops to consume.
	 */
	override fun Context<Machine>.consume(flops: Long) {
		if (finished)
			return
		if (remaining <= flops) {
			remaining = 0
		} else {
			remaining -= flops
		}
	}

	/**
	 * Finalise the task.
	 */
	override fun Context<Machine>.finalize() {
		finished = true
	}

	/**
	 * Reset the task.
	 */
	internal fun reset() {
		remaining = flops
		finished = false
	}
}
