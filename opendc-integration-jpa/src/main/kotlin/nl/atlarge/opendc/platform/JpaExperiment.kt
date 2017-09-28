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

package nl.atlarge.opendc.platform

import mu.KotlinLogging
import nl.atlarge.opendc.integration.jpa.schema.ExperimentState
import nl.atlarge.opendc.integration.jpa.schema.MachineState
import nl.atlarge.opendc.integration.jpa.schema.TaskState
import nl.atlarge.opendc.integration.jpa.schema.Experiment as InternalExperiment
import nl.atlarge.opendc.integration.jpa.schema.Task as InternalTask
import nl.atlarge.opendc.kernel.Kernel
import nl.atlarge.opendc.topology.JpaTopologyFactory
import nl.atlarge.opendc.topology.container.Rack
import nl.atlarge.opendc.topology.container.Room
import nl.atlarge.opendc.topology.destinations
import nl.atlarge.opendc.topology.machine.Machine
import javax.persistence.EntityManager

/**
 * An [Experiment] backed by the JPA API and an underlying database connection.
 *
 * @property manager The entity manager for the database connection.
 * @property experiment The internal experiment definition to use.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
class JpaExperiment(val manager: EntityManager,
					private val experiment: InternalExperiment): Experiment<Unit> {
	/**
	 * The logging instance.
	 */
	private val logger = KotlinLogging.logger {}

	/**
	 * Run the experiment on the specified simulation [Kernel].
	 *
	 * @param kernel The simulation kernel to run the experiment.
	 * @throws IllegalStateException if the simulation is already running or finished.
	 */
	override fun run(kernel: Kernel) {
		if (experiment.state != ExperimentState.CLAIMED) {
			throw IllegalStateException("The experiment is in illegal state ${experiment.state}")
		}

		// Set the simulation state
		manager.run {
			transaction.begin()
			experiment.state = ExperimentState.SIMULATING
			transaction.commit()
		}
		val section = experiment.path.sections.first()

		// Important: initialise the scheduler of the datacenter
		section.datacenter.scheduler = experiment.scheduler

		val topology = JpaTopologyFactory(section).create()
		val simulation = kernel.create(topology)
		val trace = experiment.trace

		logger.info { "Sending trace to kernel" }

		// Schedule all messages in the trace
		trace.jobs.forEach { job ->
			job.tasks.forEach { task ->
				if (task is InternalTask) {
					task.reset()
					simulation.schedule(task, section.datacenter, delay = task.startTime)
				} else {
					logger.warn { "Dropped invalid task $task" }
				}
			}
		}

		// Find all machines in the datacenter
		val machines = topology.run {
			section.datacenter.outgoingEdges.destinations<Room>("room").asSequence()
				.flatMap { it.outgoingEdges.destinations<Rack>("rack").asSequence() }
				.flatMap { it.outgoingEdges.destinations<Machine>("machine").asSequence() }.toList()
		}

		logger.info { "Starting simulation" }

		while (trace.jobs.any { !it.finished }) {
			// Collect data of simulation cycle
			manager.transaction.begin()
			machines.forEach { machine ->
				val state = simulation.run { machine.state }
				val wrapped = MachineState(0,
					machine as nl.atlarge.opendc.integration.jpa.schema.Machine,
					state.task as nl.atlarge.opendc.integration.jpa.schema.Task?,
					experiment,
					simulation.clock.now,
					state.temperature,
					state.memory,
					state.load
				)
				manager.persist(wrapped)
			}

			trace.jobs.asSequence()
				.flatMap { it.tasks.asSequence() }
				.forEach { task ->
					val state = TaskState(0,
						task as nl.atlarge.opendc.integration.jpa.schema.Task,
						experiment,
						simulation.clock.now,
						task.remaining.toInt(),
						1
					)
					manager.persist(state)
				}
			manager.transaction.commit()

			// Run next simulation cycle
			simulation.run(simulation.clock.now + 1)
			experiment.last = simulation.clock.now
		}

		// Set the experiment state
		manager.run {
			transaction.begin()
			experiment.state = ExperimentState.FINISHED
			transaction.commit()
		}

		logger.info { "Simulation done" }
	}
}
