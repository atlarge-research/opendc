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
import nl.atlarge.opendc.integration.jpa.transaction
import nl.atlarge.opendc.integration.jpa.schema.Trace as InternalTrace
import nl.atlarge.opendc.integration.jpa.schema.TaskState as InternalTaskState
import nl.atlarge.opendc.integration.jpa.schema.Experiment as InternalExperiment
import nl.atlarge.opendc.integration.jpa.schema.Task as InternalTask
import nl.atlarge.opendc.kernel.Kernel
import nl.atlarge.opendc.platform.workload.TaskState
import nl.atlarge.opendc.topology.JpaTopologyFactory
import nl.atlarge.opendc.topology.container.Rack
import nl.atlarge.opendc.topology.container.Room
import nl.atlarge.opendc.topology.destinations
import nl.atlarge.opendc.topology.machine.Machine
import java.util.*
import javax.persistence.EntityManager

/**
 * An [Experiment] backed by the JPA API and an underlying database connection.
 *
 * @property manager The entity manager for the database connection.
 * @property experiment The internal experiment definition to use.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
class JpaExperiment(private val manager: EntityManager,
					private val experiment: InternalExperiment): Experiment<Unit>, AutoCloseable {
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
		manager.transaction {
			experiment.state = ExperimentState.SIMULATING
		}

		val section = experiment.path.sections.first()

		// Important: initialise the scheduler of the datacenter
		section.datacenter.scheduler = experiment.scheduler

		val topology = JpaTopologyFactory(section).create()
		val simulation = kernel.create(topology)
		val trace = experiment.trace
		val tasks = trace.jobs.flatMap { it.tasks }

		logger.info { "Sending trace to kernel ${Objects.hashCode(trace)} ${(trace as InternalTrace).id}" }

		// Schedule all messages in the trace
		tasks.forEach { task ->
			if (task is InternalTask) {
				simulation.schedule(task, section.datacenter, delay = task.startTime)
			} else {
				logger.warn { "Dropped invalid task $task" }
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
			manager.transaction {
				experiment.last = simulation.clock.now

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
						val state = InternalTaskState(0,
							task as nl.atlarge.opendc.integration.jpa.schema.Task,
							experiment,
							simulation.clock.now,
							task.remaining.toInt(),
							1
						)
						manager.persist(state)
					}
			}

			// Run next simulation cycle
			simulation.run(simulation.clock.now + 1)
		}

		// Set the experiment state
		manager.transaction {
			experiment.state = ExperimentState.FINISHED
		}

		logger.info { "Simulation done" }
		val waiting: Long = tasks.fold(0.toLong()) { acc, task ->
			val finished = task.state as TaskState.Finished
			acc + (finished.previous.at - finished.previous.previous.at)
		} / tasks.size

		val execution: Long = tasks.fold(0.toLong()) { acc, task ->
			val finished = task.state as TaskState.Finished
			acc + (finished.at - finished.previous.at)
		} / tasks.size

		val turnaround: Long = tasks.fold(0.toLong()) { acc, task ->
			val finished = task.state as TaskState.Finished
			acc + (finished.at - finished.previous.previous.at)
		} / tasks.size

		logger.info { "Average waiting time: $waiting seconds" }
		logger.info { "Average execution time: $execution seconds" }
		logger.info { "Average turnaround time: $turnaround seconds" }
	}

	/**
	 * Closes this resource, relinquishing any underlying resources.
	 * This method is invoked automatically on objects managed by the
	 * `try`-with-resources statement.
	 *
	 * @throws Exception if this resource cannot be closed
	 */
	override fun close() = manager.close()
}
