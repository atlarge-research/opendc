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

package com.atlarge.opendc.model.odc.platform

import com.atlarge.opendc.model.odc.JpaBootstrap
import com.atlarge.opendc.model.odc.integration.jpa.schema.ExperimentState
import com.atlarge.opendc.model.odc.integration.jpa.schema.MachineState
import com.atlarge.opendc.model.odc.integration.jpa.transaction
import com.atlarge.opendc.model.odc.platform.workload.TaskState
import com.atlarge.opendc.model.odc.topology.container.Rack
import com.atlarge.opendc.model.odc.topology.container.Room
import com.atlarge.opendc.model.odc.topology.machine.Machine
import com.atlarge.opendc.model.topology.destinations
import com.atlarge.opendc.simulator.Duration
import com.atlarge.opendc.simulator.kernel.KernelFactory
import com.atlarge.opendc.simulator.platform.Experiment
import mu.KotlinLogging
import java.io.Closeable
import javax.persistence.EntityManager
import com.atlarge.opendc.model.odc.integration.jpa.schema.Experiment as InternalExperiment
import com.atlarge.opendc.model.odc.integration.jpa.schema.Task as InternalTask
import com.atlarge.opendc.model.odc.integration.jpa.schema.TaskState as InternalTaskState
import com.atlarge.opendc.model.odc.integration.jpa.schema.Trace as InternalTrace

/**
 * An [Experiment] backed by the JPA API and an underlying database connection.
 *
 * @property manager The entity manager for the database connection.
 * @property experiment The internal experiment definition to use.
 * @author Fabian Mastenbroek (f.s.mastenbroek@student.tudelft.nl)
 */
class JpaExperiment(private val manager: EntityManager,
                    private val experiment: InternalExperiment) : Experiment<Unit>, Closeable {
    /**
     * The logging instance.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * Run the experiment using the specified simulation kernel implementation.
     *
     * @param factory The simulation kernel implementation to use.
     * @param timeout The maximum duration of the experiment before returning to the caller.
     * @return The result of the experiment or `null`.
     */
    override fun run(factory: KernelFactory, timeout: Duration): Unit? {
        if (experiment.state != ExperimentState.CLAIMED) {
            throw IllegalStateException("The experiment is in illegal state ${experiment.state}")
        }

        // Set the simulation state
        manager.transaction {
            experiment.state = ExperimentState.SIMULATING
        }

        val bootstrap = JpaBootstrap(experiment)
        val simulation = factory.create(bootstrap)
        val topology = simulation.model

        val section = experiment.path.sections.first()
        val trace = experiment.trace
        val tasks = trace.jobs.flatMap { it.tasks }

        // Find all machines in the datacenter
        val machines = topology.run {
            section.datacenter.outgoingEdges.destinations<Room>("room").asSequence()
                .flatMap { it.outgoingEdges.destinations<Rack>("rack").asSequence() }
                .flatMap { it.outgoingEdges.destinations<Machine>("machine").asSequence() }.toList()
        }

        logger.info { "Starting simulation" }

        while (trace.jobs.any { !it.finished }) {
            // If we have reached a timeout, return
            if (simulation.time >= timeout)
                return null

            // Collect data of simulation cycle
            manager.transaction {
                experiment.last = simulation.time

                machines.forEach { machine ->
                    val state = simulation.run { machine.state }
                    val wrapped = MachineState(0,
                        machine as com.atlarge.opendc.model.odc.integration.jpa.schema.Machine,
                        state.task as com.atlarge.opendc.model.odc.integration.jpa.schema.Task?,
                        experiment,
                        simulation.time,
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
                            task as com.atlarge.opendc.model.odc.integration.jpa.schema.Task,
                            experiment,
                            simulation.time,
                            task.remaining.toInt(),
                            1
                        )
                        manager.persist(state)
                    }
            }

            // Run next simulation cycle
            simulation.run(simulation.time + 1)
        }

        // Set the experiment state
        manager.transaction {
            experiment.state = ExperimentState.FINISHED
        }

        logger.info { "Kernel done" }
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

        return Unit
    }

    /**
     * Run the experiment on the specified simulation kernel implementation.
     *
     * @param factory The factory to create the simulation kernel with.
     * @throws IllegalStateException if the simulation is already running or finished.
     */
    override fun run(factory: KernelFactory) = run(factory, -1)!!

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * `try`-with-resources statement.
     *
     * @throws Exception if this resource cannot be closed
     */
    override fun close() = manager.close()
}
