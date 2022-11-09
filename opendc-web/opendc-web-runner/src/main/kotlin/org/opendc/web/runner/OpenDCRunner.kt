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
import org.opendc.compute.service.ComputeService
import org.opendc.experiments.compute.ComputeWorkloadLoader
import org.opendc.experiments.compute.createComputeScheduler
import org.opendc.experiments.compute.grid5000
import org.opendc.experiments.compute.registerComputeMonitor
import org.opendc.experiments.compute.replay
import org.opendc.experiments.compute.sampleByLoad
import org.opendc.experiments.compute.setupComputeService
import org.opendc.experiments.compute.setupHosts
import org.opendc.experiments.compute.topology.HostSpec
import org.opendc.experiments.compute.trace
import org.opendc.experiments.provisioner.Provisioner
import org.opendc.simulator.compute.SimPsuFactories
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.CpuPowerModels
import org.opendc.simulator.kotlin.runSimulation
import org.opendc.web.proto.runner.Job
import org.opendc.web.proto.runner.Scenario
import org.opendc.web.proto.runner.Topology
import org.opendc.web.runner.internal.WebComputeMonitor
import java.io.File
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.Random
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.ForkJoinPool
import java.util.concurrent.ForkJoinPool.ForkJoinWorkerThreadFactory
import java.util.concurrent.ForkJoinWorkerThread
import java.util.concurrent.RecursiveAction
import java.util.concurrent.RecursiveTask
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

/**
 * Class to execute the pending jobs via the OpenDC web API.
 *
 * @param manager The underlying [JobManager] to manage the available jobs.
 * @param tracePath The directory where the traces are located.
 * @param jobTimeout The maximum duration of a simulation job.
 * @param pollInterval The interval to poll the API with.
 * @param heartbeatInterval The interval to send a heartbeat to the API server.
 */
public class OpenDCRunner(
    private val manager: JobManager,
    private val tracePath: File,
    parallelism: Int = Runtime.getRuntime().availableProcessors(),
    private val jobTimeout: Duration = Duration.ofMinutes(10),
    private val pollInterval: Duration = Duration.ofSeconds(30),
    private val heartbeatInterval: Duration = Duration.ofMinutes(1)
) : Runnable {
    /**
     * Logging instance for this runner.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * Helper class to load the workloads.
     */
    private val workloadLoader = ComputeWorkloadLoader(tracePath)

    /**
     * The [ForkJoinPool] that is used to execute the simulation jobs.
     */
    private val pool =
        ForkJoinPool(parallelism, RunnerThreadFactory(Thread.currentThread().contextClassLoader), null, false)

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
        } catch (_: InterruptedException) {
            // Gracefully exit when the thread is interrupted
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
            val startTime = Instant.now()
            val currentThread = Thread.currentThread()

            val heartbeat = scheduler.scheduleWithFixedDelay(
                {
                    if (!manager.heartbeat(id, startTime.secondsSince())) {
                        currentThread.interrupt()
                    }
                },
                0,
                heartbeatInterval.toMillis(),
                TimeUnit.MILLISECONDS
            )

            try {
                val topology = convertTopology(scenario.topology)
                val jobs = (0 until scenario.portfolio.targets.repeats).map { repeat ->
                    SimulationTask(
                        scenario,
                        repeat,
                        topology
                    )
                }
                val results = invokeAll(jobs).map { it.rawResult }

                heartbeat.cancel(true)

                val duration = startTime.secondsSince()
                logger.info { "Finished simulation for job $id (in $duration seconds)" }

                manager.finish(
                    id,
                    duration,
                    mapOf(
                        "total_requested_burst" to results.map { it.totalActiveTime + it.totalIdleTime },
                        "total_granted_burst" to results.map { it.totalActiveTime },
                        "total_overcommitted_burst" to results.map { it.totalStealTime },
                        "total_interfered_burst" to results.map { it.totalLostTime },
                        "mean_cpu_usage" to results.map { it.meanCpuUsage },
                        "mean_cpu_demand" to results.map { it.meanCpuDemand },
                        "mean_num_deployed_images" to results.map { it.meanNumDeployedImages },
                        "max_num_deployed_images" to results.map { it.maxNumDeployedImages },
                        "total_power_draw" to results.map { it.totalPowerDraw },
                        "total_failure_slices" to results.map { it.totalFailureSlices },
                        "total_failure_vm_slices" to results.map { it.totalFailureVmSlices },
                        "total_vms_submitted" to results.map { it.totalVmsSubmitted },
                        "total_vms_queued" to results.map { it.totalVmsQueued },
                        "total_vms_finished" to results.map { it.totalVmsFinished },
                        "total_vms_failed" to results.map { it.totalVmsFailed }
                    )
                )
            } catch (e: Exception) {
                // Check whether the job failed due to exceeding its time budget
                if (Thread.interrupted()) {
                    logger.info { "Simulation job $id exceeded time limit (${startTime.secondsSince()} seconds)" }
                } else {
                    logger.info(e) { "Simulation job $id failed" }
                }

                try {
                    heartbeat.cancel(true)
                    manager.fail(id, startTime.secondsSince())
                } catch (e: Throwable) {
                    logger.error(e) { "Failed to update job" }
                }
            }
        }

        /**
         * Calculate the seconds since the specified instant.
         */
        private fun Instant.secondsSince(): Int {
            return ChronoUnit.SECONDS.between(this, Instant.now()).toInt()
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
        private val topology: List<HostSpec>
    ) : RecursiveTask<WebComputeMonitor.Results>() {
        override fun compute(): WebComputeMonitor.Results {
            val monitor = WebComputeMonitor()

            // Schedule task that interrupts the simulation if it runs for too long.
            val currentThread = Thread.currentThread()
            val interruptTask =
                scheduler.schedule({ currentThread.interrupt() }, jobTimeout.toMillis(), TimeUnit.MILLISECONDS)

            try {
                runSimulation(monitor)
            } finally {
                interruptTask.cancel(false)
            }

            return monitor.collectResults()
        }

        /**
         * Run a single simulation of the scenario.
         */
        private fun runSimulation(monitor: WebComputeMonitor) = runSimulation {
            val serviceDomain = "compute.opendc.org"
            val seed = repeat.toLong()

            val scenario = scenario

            Provisioner(dispatcher, seed).use { provisioner ->
                provisioner.runSteps(
                    setupComputeService(
                        serviceDomain,
                        { createComputeScheduler(scenario.schedulerName, Random(it.seeder.nextLong())) }
                    ),
                    registerComputeMonitor(serviceDomain, monitor),
                    setupHosts(serviceDomain, topology)
                )

                val service = provisioner.registry.resolve(serviceDomain, ComputeService::class.java)!!

                val workload =
                    trace(scenario.workload.trace.id).sampleByLoad(scenario.workload.samplingFraction)
                val vms = workload.resolve(workloadLoader, Random(seed))

                val phenomena = scenario.phenomena
                val failureModel =
                    if (phenomena.failures) {
                        grid5000(Duration.ofDays(7))
                    } else {
                        null
                    }

                // Run workload trace
                service.replay(timeSource, vms, seed, failureModel = failureModel, interference = phenomena.interference)

                val serviceMetrics = service.getSchedulerStats()
                logger.debug {
                    "Scheduler " +
                        "Success=${serviceMetrics.attemptsSuccess} " +
                        "Failure=${serviceMetrics.attemptsFailure} " +
                        "Error=${serviceMetrics.attemptsError} " +
                        "Pending=${serviceMetrics.serversPending} " +
                        "Active=${serviceMetrics.serversActive}"
                }
            }
        }
    }

    /**
     * Convert the specified [topology] into an [Topology] understood by OpenDC.
     */
    private fun convertTopology(topology: Topology): List<HostSpec> {
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
            val powerModel = CpuPowerModels.linear(2 * energyConsumptionW, energyConsumptionW * 0.5)

            val spec = HostSpec(
                UUID(random.nextLong(), random.nextLong()),
                "node-$clusterId-$position",
                mapOf("cluster" to clusterId),
                MachineModel(processors, memoryUnits),
                SimPsuFactories.simple(powerModel)
            )

            res += spec
        }

        return res
    }

    /**
     * A custom [ForkJoinWorkerThreadFactory] that uses the [ClassLoader] of specified by the runner.
     */
    private class RunnerThreadFactory(private val classLoader: ClassLoader) : ForkJoinWorkerThreadFactory {
        override fun newThread(pool: ForkJoinPool): ForkJoinWorkerThread = object : ForkJoinWorkerThread(pool) {
            init {
                contextClassLoader = classLoader
            }
        }
    }
}
