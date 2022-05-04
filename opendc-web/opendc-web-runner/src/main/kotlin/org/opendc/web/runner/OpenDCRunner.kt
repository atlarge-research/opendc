/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.web.runner

import mu.KotlinLogging
import org.opendc.compute.api.Server
import org.opendc.compute.workload.*
import org.opendc.compute.workload.telemetry.ComputeMetricReader
import org.opendc.compute.workload.topology.HostSpec
import org.opendc.compute.workload.topology.Topology
import org.opendc.compute.workload.topology.apply
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.LinearPowerModel
import org.opendc.simulator.compute.power.SimplePowerDriver
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.web.client.runner.OpenDCRunnerClient
import org.opendc.web.proto.runner.Job
import org.opendc.web.proto.runner.Scenario
import org.opendc.web.runner.internal.JobManager
import org.opendc.web.runner.internal.WebComputeMonitor
import java.io.File
import java.time.Duration
import java.util.*
import java.util.concurrent.*

/**
 * Class to execute the pending jobs via the OpenDC web API.
 *
 * @param client The [OpenDCRunnerClient] to connect to the OpenDC web API.
 * @param tracePath The directory where the traces are located.
 * @param jobTimeout The maximum duration of a simulation job.
 * @param pollInterval The interval to poll the API with.
 * @param heartbeatInterval The interval to send a heartbeat to the API server.
 */
public class OpenDCRunner(
    client: OpenDCRunnerClient,
    private val tracePath: File,
    parallelism: Int = Runtime.getRuntime().availableProcessors(),
    private val jobTimeout: Duration = Duration.ofMillis(10),
    private val pollInterval: Duration = Duration.ofSeconds(30),
    private val heartbeatInterval: Duration = Duration.ofMinutes(1)
) : Runnable {
    /**
     * Logging instance for this runner.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * Helper class to manage the available jobs.
     */
    private val manager = JobManager(client)

    /**
     * Helper class to load the workloads.
     */
    private val workloadLoader = ComputeWorkloadLoader(tracePath)

    /**
     * The [ForkJoinPool] that is used to execute the simulation jobs.
     */
    private val pool = ForkJoinPool(parallelism)

    /**
     * A [ScheduledExecutorService] to manage the heartbeat of simulation jobs as well as tracking the deadline of
     * individual simulations.
     */
    private val scheduler = Executors.newSingleThreadScheduledExecutor()

    /**
     * Start the runner process.
     *
     * This method will block until interrupted and poll the OpenDC API for new jobs to execute.
     */
    override fun run() {
        try {
            while (true) {
                // Check if anyone has interrupted the thread
                if (Thread.interrupted()) {
                    throw InterruptedException()
                }

                val job = manager.findNext()
                if (job == null) {
                    Thread.sleep(pollInterval.toMillis())
                    continue
                }

                val id = job.id

                logger.info { "Found queued job $id: attempting to claim" }

                if (!manager.claim(id)) {
                    logger.info { "Failed to claim scenario" }
                    continue
                }

                pool.submit(JobAction(job))
            }
        } finally {
            workloadLoader.reset()

            pool.shutdown()
            scheduler.shutdown()

            pool.awaitTermination(5, TimeUnit.SECONDS)
        }
    }

    /**
     * A [RecursiveAction] that runs a simulation job (consisting of possible multiple simulations).
     *
     * @param job The job to simulate.
     */
    private inner class JobAction(private val job: Job) : RecursiveAction() {
        override fun compute() {
            val id = job.id
            val scenario = job.scenario

            val heartbeat = scheduler.scheduleWithFixedDelay({ manager.heartbeat(id) }, 0, heartbeatInterval.toMillis(), TimeUnit.MILLISECONDS)

            try {
                val topology = convertTopology(scenario.topology)
                val jobs = (0 until scenario.portfolio.targets.repeats).map { repeat -> SimulationTask(scenario, repeat, topology) }
                val results = invokeAll(jobs)

                logger.info { "Finished simulation for job $id" }

                heartbeat.cancel(true)
                manager.finish(id, results.map { it.rawResult })
            } catch (e: Exception) {
                // Check whether the job failed due to exceeding its time budget
                if (Thread.interrupted()) {
                    logger.info { "Simulation job $id exceeded time limit" }
                } else {
                    logger.info(e) { "Simulation job $id failed" }
                }

                try {
                    heartbeat.cancel(true)
                    manager.fail(id)
                } catch (e: Throwable) {
                    logger.error(e) { "Failed to update job" }
                }
            }
        }
    }

    /**
     * A [RecursiveTask] that simulates a single scenario.
     *
     * @param scenario The scenario to simulate.
     * @param repeat The repeat number used to seed the simulation.
     * @param topology The topology to simulate.
     */
    private inner class SimulationTask(
        private val scenario: Scenario,
        private val repeat: Int,
        private val topology: Topology,
    ) : RecursiveTask<WebComputeMonitor.Results>() {
        override fun compute(): WebComputeMonitor.Results {
            val monitor = WebComputeMonitor()

            // Schedule task that interrupts the simulation if it runs for too long.
            val currentThread = Thread.currentThread()
            val interruptTask = scheduler.schedule({ currentThread.interrupt() }, jobTimeout.toMillis(), TimeUnit.MILLISECONDS)

            try {
                runBlockingSimulation {
                    val workloadName = scenario.workload.trace.id
                    val workloadFraction = scenario.workload.samplingFraction

                    val seeder = Random(repeat.toLong())

                    val phenomena = scenario.phenomena
                    val computeScheduler = createComputeScheduler(scenario.schedulerName, seeder)
                    val workload = trace(workloadName).sampleByLoad(workloadFraction)
                    val (vms, interferenceModel) = workload.resolve(workloadLoader, seeder)

                    val failureModel =
                        if (phenomena.failures)
                            grid5000(Duration.ofDays(7))
                        else
                            null

                    val simulator = ComputeServiceHelper(
                        coroutineContext,
                        clock,
                        computeScheduler,
                        failureModel,
                        interferenceModel.takeIf { phenomena.interference }
                    )
                    val servers = mutableListOf<Server>()
                    val reader = ComputeMetricReader(this, clock, simulator.service, servers, monitor)

                    try {
                        // Instantiate the topology onto the simulator
                        simulator.apply(topology)
                        // Run workload trace
                        simulator.run(vms, seeder.nextLong(), servers)

                        val serviceMetrics = simulator.service.getSchedulerStats()
                        logger.debug {
                            "Scheduler " +
                                "Success=${serviceMetrics.attemptsSuccess} " +
                                "Failure=${serviceMetrics.attemptsFailure} " +
                                "Error=${serviceMetrics.attemptsError} " +
                                "Pending=${serviceMetrics.serversPending} " +
                                "Active=${serviceMetrics.serversActive}"
                        }
                    } finally {
                        simulator.close()
                        reader.close()
                    }
                }
            } finally {
                interruptTask.cancel(false)
            }

            return monitor.collectResults()
        }
    }

    /**
     * Convert the specified [topology] into an [Topology] understood by OpenDC.
     */
    private fun convertTopology(topology: org.opendc.web.proto.runner.Topology): Topology {
        return object : Topology {

            override fun resolve(): List<HostSpec> {
                val res = mutableListOf<HostSpec>()
                val random = Random(0)

                val machines = topology.rooms.asSequence()
                    .flatMap { room ->
                        room.tiles.flatMap { tile ->
                            val rack = tile.rack
                            rack?.machines?.map { machine -> rack to machine } ?: emptyList()
                        }
                    }
                for ((rack, machine) in machines) {
                    val clusterId = rack.id
                    val position = machine.position

                    val processors = machine.cpus.flatMap { cpu ->
                        val cores = cpu.numberOfCores
                        val speed = cpu.clockRateMhz
                        // TODO Remove hard coding of vendor
                        val node = ProcessingNode("Intel", "amd64", cpu.name, cores)
                        List(cores) { coreId ->
                            ProcessingUnit(node, coreId, speed)
                        }
                    }
                    val memoryUnits = machine.memory.map { memory ->
                        MemoryUnit(
                            "Samsung",
                            memory.name,
                            memory.speedMbPerS,
                            memory.sizeMb.toLong()
                        )
                    }

                    val energyConsumptionW = machine.cpus.sumOf { it.energyConsumptionW }
                    val powerModel = LinearPowerModel(2 * energyConsumptionW, energyConsumptionW * 0.5)
                    val powerDriver = SimplePowerDriver(powerModel)

                    val spec = HostSpec(
                        UUID(random.nextLong(), random.nextLong()),
                        "node-$clusterId-$position",
                        mapOf("cluster" to clusterId),
                        MachineModel(processors, memoryUnits),
                        powerDriver
                    )

                    res += spec
                }

                return res
            }

            override fun toString(): String = "WebRunnerTopologyFactory"
        }
    }
}
