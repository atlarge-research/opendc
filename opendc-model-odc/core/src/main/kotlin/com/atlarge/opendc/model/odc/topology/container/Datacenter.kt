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

package com.atlarge.opendc.model.odc.topology.container

import mu.KotlinLogging
import com.atlarge.opendc.simulator.Context
import com.atlarge.opendc.simulator.Duration
import com.atlarge.opendc.simulator.Entity
import com.atlarge.opendc.simulator.Process
import com.atlarge.opendc.model.odc.platform.scheduler.Scheduler
import com.atlarge.opendc.model.odc.platform.workload.Task
import com.atlarge.opendc.model.odc.topology.machine.Machine
import com.atlarge.opendc.model.topology.Topology
import com.atlarge.opendc.model.topology.destinations
import java.util.*

/**
 * A representation of a facility used to house computer systems and associated components.
 *
 * @property scheduler The tasks scheduler the datacenter uses.
 * @property interval The interval at which task will be (re)scheduled.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
interface Datacenter : Process<Unit, Topology> {
	/**
	 * The task scheduler the datacenter uses.
	 */
	val scheduler: Scheduler

	/**
	 * The interval at which task will be (re)scheduled.
	 */
	val interval: Duration

	/**
	 * This method is invoked to start the simulation an [Entity] associated with this [Context].
	 *
	 * This method is assumed to be running during a simulation, but should hand back control to the simulator at
	 * some point by suspending the process. This allows other processes to do work in the current tick of the
	 * simulation.
	 * Suspending the process can be achieved by calling suspending method in the context:
	 * 	- [Context.hold]	- Wait for `n` amount of ticks before resuming execution.
	 * 	- [Context.receive]	- Wait for a message to be received in the mailbox of the [Entity] before resuming
	 * 	execution.
	 *
	 * If this method exits early, before the simulation has finished, the entity is assumed to be shutdown and its
	 * simulation will not run any further.
	 */
	suspend override fun Context<Unit, Topology>.run() = model.run {
		val logger = KotlinLogging.logger {}

		// The queue of messages to be processed after a cycle
		val queue: Queue<Any> = ArrayDeque()
		// Find all machines in the datacenter
		val machines = outgoingEdges.destinations<Room>("room").asSequence()
			.flatMap { it.outgoingEdges.destinations<Rack>("rack").asSequence() }
			.flatMap { it.outgoingEdges.destinations<Machine>("machine").asSequence() }.toList()

		logger.info { "Initialising datacenter with ${machines.size} machines" }

		// Register all machines to the scheduler
		machines.forEach(scheduler::register)

		while (true) {
			// Context all messages in the queue
			while (queue.isNotEmpty()) {
				val msg = queue.poll()
				if (msg is Task) {
					msg.arrive(time)
					scheduler.submit(msg)
				}
			}
			// (Re)schedule the tasks
			scheduler.run { schedule() }

			// Sleep a time quantum
			hold(interval, queue)
		}
	}
}
