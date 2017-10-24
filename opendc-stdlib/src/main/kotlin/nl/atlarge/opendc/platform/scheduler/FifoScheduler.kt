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

package nl.atlarge.opendc.platform.scheduler

import nl.atlarge.opendc.kernel.Context
import nl.atlarge.opendc.platform.workload.Job
import nl.atlarge.opendc.topology.Entity
import nl.atlarge.opendc.topology.machine.Machine
import nl.atlarge.opendc.platform.workload.Task
import java.util.*

/**
 * A [Scheduler] that distributes work according to the first-in-first-out principle.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
class FifoScheduler : Scheduler {
	/**
	 * The name of this scheduler.
	 */
	override val name: String = "FIFO"

	/**
	 * The set of machines the scheduler knows of.
	 */
	val machines: MutableSet<Machine> = HashSet()

	/**
	 * The queue of [Task]s that need to be scheduled.
	 */
	val queue: Queue<Task> = ArrayDeque()

	/**
	 * (Re)schedule the tasks submitted to the scheduler over the specified set of machines.
	 */
	override suspend fun <E : Entity<*>> Context<E>.schedule() {
		if (queue.isEmpty()) {
			return
		}

		// The tasks that need to be rescheduled
		val rescheduled = ArrayDeque<Task>()
		val iterator = queue.iterator()

		machines
			.filter { it.state.status != Machine.Status.HALT }
			.forEach { machine ->
				while (iterator.hasNext()) {
					val task = iterator.next()

					// TODO What to do with tasks that are not ready yet to be processed
					if (!task.ready) {
						iterator.remove()
						rescheduled.add(task)
						continue
					} else if (task.finished) {
						iterator.remove()
						continue
					}

					machine.send(task)
					break
				}
			}

		// Reschedule all tasks that are not ready yet
		while (!rescheduled.isEmpty()) {
			queue.add(rescheduled.poll())
		}
	}

	/**
	 * Submit a [Task] to this scheduler.
	 *
	 * @param task The task to submit to the scheduler.
	 */
	override fun submit(task: Task) {
		queue.add(task)
	}

	/**
	 * Register a [Machine] to this scheduler.
	 *
	 * @param machine The machine to register.
	 */
	override fun register(machine: Machine) {
		machines.add(machine)
	}

	/**
	 * Deregister a [Machine] from this scheduler.
	 *
	 * @param machine The machine to deregister.
	 */
	override fun deregister(machine: Machine) {
		machines.remove(machine)
	}
}
