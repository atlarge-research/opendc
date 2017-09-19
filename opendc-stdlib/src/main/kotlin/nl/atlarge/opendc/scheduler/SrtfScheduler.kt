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

package nl.atlarge.opendc.scheduler

import nl.atlarge.opendc.kernel.Context
import nl.atlarge.opendc.topology.Entity
import nl.atlarge.opendc.topology.machine.Machine
import nl.atlarge.opendc.workload.Task
import java.util.*

/**
 * A [Scheduler] that distributes work according to the shortest job first policy.
 *
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
class SrtfScheduler : Scheduler {
	/**
	 * The set of machines the scheduler knows of.
	 */
	val machines: MutableSet<Machine> = HashSet()

	/**
	 * The set of [Task]s that need to be scheduled.
	 */
	val tasks: MutableSet<Task> = HashSet()

	/**
	 * (Re)schedule the tasks submitted to the scheduler over the specified set of machines.
	 */
	override suspend fun <E : Entity<*>> Context<E>.schedule() {
		if (tasks.isEmpty()) {
			return
		}

		val iterator = tasks.sortedBy { it.remaining }.iterator()

		machines
			.filter { it.state.status == Machine.Status.IDLE }
			.forEach {
				while (iterator.hasNext()) {
					val task = iterator.next()

					// TODO What to do with tasks that are not ready yet to be processed
					if (!task.isReady()) {
						submit(task)
						continue
					} else if (task.finished) {
						tasks.remove(task)
						continue
					}

					it.send(task)
					break
				}
			}
	}

	/**
	 * Submit a [Task] to this scheduler.
	 *
	 * @param task The task to submit to the scheduler.
	 */
	override fun submit(task: Task) {
		tasks.add(task)
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
