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
import com.atlarge.opendc.model.odc.JpaModel
import com.atlarge.opendc.model.odc.integration.jpa.persist
import com.atlarge.opendc.model.odc.integration.jpa.schema.ExperimentState
import com.atlarge.opendc.model.odc.integration.jpa.schema.interpolate
import com.atlarge.opendc.model.odc.integration.jpa.schema.MachineState
import com.atlarge.opendc.model.odc.integration.jpa.transaction
import com.atlarge.opendc.model.odc.platform.workload.Task
import com.atlarge.opendc.model.odc.platform.workload.TaskState
import com.atlarge.opendc.model.odc.topology.container.Rack
import com.atlarge.opendc.model.odc.topology.container.Room
import com.atlarge.opendc.model.odc.topology.machine.Machine
import com.atlarge.opendc.model.topology.destinations
import com.atlarge.opendc.simulator.Duration
import com.atlarge.opendc.simulator.instrumentation.*
import com.atlarge.opendc.simulator.kernel.Kernel
import com.atlarge.opendc.simulator.platform.Experiment
import kotlinx.coroutines.experimental.channels.Channel
import kotlinx.coroutines.experimental.channels.asReceiveChannel
import kotlinx.coroutines.experimental.channels.map
import kotlinx.coroutines.experimental.launch
import kotlinx.coroutines.experimental.runBlocking
import mu.KotlinLogging
import java.io.Closeable
import javax.persistence.EntityManager
import kotlin.coroutines.experimental.coroutineContext
import kotlin.system.measureTimeMillis
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
    override fun run(factory: Kernel, timeout: Duration): Unit? {
        if (experiment.state != ExperimentState.CLAIMED) {
            throw IllegalStateException("The experiment is in illegal state ${experiment.state}")
        }

        logger.info { "Initialising experiment ${experiment.id}" }

        // Set the simulation state
        manager.transaction {
            experiment.state = ExperimentState.SIMULATING
        }

        val bootstrap = JpaBootstrap(experiment)
        val simulation = factory.create(bootstrap)

        val section = experiment.path.sections.first()
        val trace = experiment.trace
        val tasks = trace.jobs.flatMap { it.tasks }

        // The port we use to install the instruments
        val port = simulation.openPort()

        // Find all machines in the datacenter
        val machines = simulation.model.run {
            section.datacenter.outgoingEdges.destinations<Room>("room").asSequence()
                .flatMap { it.outgoingEdges.destinations<Rack>("rack").asSequence() }
                .flatMap { it.outgoingEdges.destinations<Machine>("machine").asSequence() }.toList()
        }

        // The instrument used for monitoring machines
        fun machine(machine: Machine): Instrument<MachineState, JpaModel> = {
            while (true) {
                send(MachineState(
                    0,
                    machine as com.atlarge.opendc.model.odc.integration.jpa.schema.Machine,
                    machine.state.task as InternalTask?,
                    experiment,
                    time,
                    machine.state.temperature,
                    machine.state.memory,
                    machine.state.load
                ))
                hold(10)
            }
        }

        // The stream of machine state measurements
        val machineStates = machines
            .asReceiveChannel()
            .map { machine(it) }
            .flatMapMerge { port.install(Channel.UNLIMITED, it).interpolate(9) }

        // The instrument used for monitoring tasks
        fun task(task: Task): Instrument<InternalTaskState, JpaModel> = {
            while (true) {
                send(InternalTaskState(
                    0,
                    task as InternalTask,
                    experiment,
                    time,
                    task.remaining.toInt(),
                    1
                ))

                hold(10)
            }
        }

        // The stream of task state measurements
        val taskStates = tasks
            .asReceiveChannel()
            .map { task(it) }
            .flatMapMerge { port.install(it).interpolate(9) }

        // A job which writes the data to database in a separate thread
        val writer = launch {
            taskStates.merge(coroutineContext, machineStates)
                .persist(manager.entityManagerFactory)
        }

        // A method to flush the remaining measurements to the database
        fun finalize() = runBlocking {
            logger.info { "Flushing remaining measurements to database" }

            // Stop gathering new measurements
            port.close()

            // Wait for writer thread to finish
            writer.join()
        }

        logger.info { "Starting simulation" }
        logger.info { "Scheduling total of ${trace.jobs.size} jobs and ${tasks.size} tasks" }

        val measurement = measureTimeMillis {
            while (true) {
                // Have all jobs finished yet
                if (trace.jobs.all { it.finished })
                    break

                // If we have reached a timeout, return
                if (simulation.time >= timeout) {
                    // Flush remaining data
                    finalize()

                    // Mark the experiment as aborted
                    manager.transaction {
                        experiment.last = simulation.time
                        experiment.state = ExperimentState.ABORTED
                    }

                    logger.warn { "Experiment aborted due to timeout" }
                    return null
                }

                try {
                    // Run next simulation cycle
                    simulation.step()
                } catch (e: Throwable) {
                    logger.error(e) { "An error occurred during execution of the experiment" }
                }
            }
        }

        logger.info { "Simulation done in $measurement milliseconds" }

        // Flush remaining data to database
        finalize()

        // Mark experiment as finished
        manager.transaction {
            experiment.last = simulation.time
            experiment.state = ExperimentState.FINISHED
        }

        logger.info { "Computing statistics" }
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
    override fun run(factory: Kernel) = run(factory, -1)!!

    /**
     * Closes this resource, relinquishing any underlying resources.
     * This method is invoked automatically on objects managed by the
     * `try`-with-resources statement.
     *
     * @throws Exception if this resource cannot be closed
     */
    override fun close() = manager.close()
}
