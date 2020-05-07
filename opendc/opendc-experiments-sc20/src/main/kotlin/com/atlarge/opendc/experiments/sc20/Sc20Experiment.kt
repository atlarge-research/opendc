/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.opendc.experiments.sc20

import com.atlarge.odcsim.Domain
import com.atlarge.odcsim.SimulationEngineProvider
import com.atlarge.odcsim.simulationContext
import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.ServerEvent
import com.atlarge.opendc.compute.core.workload.PerformanceInterferenceModel
import com.atlarge.opendc.compute.core.workload.VmWorkload
import com.atlarge.opendc.compute.metal.NODE_CLUSTER
import com.atlarge.opendc.compute.metal.service.ProvisioningService
import com.atlarge.opendc.compute.virt.HypervisorEvent
import com.atlarge.opendc.compute.virt.driver.SimpleVirtDriver
import com.atlarge.opendc.compute.virt.service.SimpleVirtProvisioningService
import com.atlarge.opendc.compute.virt.service.allocation.AllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.AvailableCoreMemoryAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.AvailableMemoryAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.NumberOfActiveServersAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.ProvisionedCoresAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.RandomAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.ReplayAllocationPolicy
import com.atlarge.opendc.core.failure.CorrelatedFaultInjector
import com.atlarge.opendc.core.failure.FailureDomain
import com.atlarge.opendc.core.failure.FaultInjector
import com.atlarge.opendc.format.environment.EnvironmentReader
import com.atlarge.opendc.format.environment.sc20.Sc20ClusterEnvironmentReader
import com.atlarge.opendc.format.trace.TraceReader
import com.atlarge.opendc.format.trace.sc20.Sc20PerformanceInterferenceReader
import com.atlarge.opendc.format.trace.sc20.Sc20VmPlacementReader
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import mu.KotlinLogging
import java.io.File
import java.io.FileReader
import java.util.ServiceLoader
import java.util.TreeSet
import kotlin.math.ln
import kotlin.math.max
import kotlin.random.Random

private val logger = KotlinLogging.logger {}

class ExperimentParameters(parser: ArgParser) {
    val traceDirectory by parser.storing("path to the trace directory")
    val environmentFile by parser.storing("path to the environment file")
    val performanceInterferenceFile by parser.storing("path to the performance interference file").default { null }
    val vmPlacementFile by parser.storing("path to the VM placement file").default { null }
    val outputFile by parser.storing("path to where the output should be stored")
        .default { "data/results-${System.currentTimeMillis()}.parquet" }
    val selectedVms by parser.storing("the VMs to run") { parseVMs(this) }
        .default { emptyList() }
    val selectedVmsFile by parser.storing("path to a file containing the VMs to run") {
        parseVMs(FileReader(File(this)).readText())
    }
        .default { emptyList() }
    val seed by parser.storing("the random seed") { toInt() }
        .default(0)
    val failures by parser.flagging("-x", "--failures", help = "enable (correlated) machine failures")
    val failureInterval by parser.storing("expected number of hours between failures") { toInt() }
        .default(24 * 7) // one week
    val allocationPolicy by parser.storing("name of VM allocation policy to use").default("core-mem")

    fun getSelectedVmList(): List<String> {
        return if (selectedVms.isEmpty()) {
            selectedVmsFile
        } else {
            selectedVms
        }
    }

    private fun parseVMs(string: String): List<String> {
        // Handle case where VM list contains a VM name with an (escaped) single-quote in it
        val sanitizedString = string.replace("\\'", "\\\\[")
            .replace("'", "\"")
            .replace("\\\\[", "'")
        val vms: List<String> = jacksonObjectMapper().readValue(sanitizedString)
        return vms
    }
}

/**
 * Construct the failure domain for the experiments.
 */
suspend fun createFailureDomain(
    seed: Int,
    failureInterval: Int,
    bareMetalProvisioner: ProvisioningService,
    chan: Channel<Unit>
): Domain {
    val root = simulationContext.domain
    val domain = root.newDomain(name = "failures")
    domain.launch {
        chan.receive()
        val random = Random(seed)
        val injectors = mutableMapOf<String, FaultInjector>()
        for (node in bareMetalProvisioner.nodes()) {
            val cluster = node.metadata[NODE_CLUSTER] as String
            val injector =
                injectors.getOrPut(cluster) { createFaultInjector(simulationContext.domain, random, failureInterval) }
            injector.enqueue(node.metadata["driver"] as FailureDomain)
        }
    }
    return domain
}

/**
 * Obtain the [FaultInjector] to use for the experiments.
 */
fun createFaultInjector(domain: Domain, random: Random, failureInterval: Int): FaultInjector {
    // Parameters from A. Iosup, A Framework for the Study of Grid Inter-Operation Mechanisms, 2009
    // GRID'5000
    return CorrelatedFaultInjector(
        domain,
        iatScale = ln(failureInterval.toDouble()), iatShape = 1.03, // Hours
        sizeScale = 1.88, sizeShape = 1.25,
        dScale = 9.51, dShape = 3.21, // Minutes
        random = random
    )
}

/**
 * Create the trace reader from which the VM workloads are read.
 */
fun createTraceReader(path: File, performanceInterferenceModel: PerformanceInterferenceModel, vms: List<String>, seed: Int): Sc20ParquetTraceReader {
    return Sc20ParquetTraceReader(path, performanceInterferenceModel, vms, Random(seed))
}

/**
 * Construct the environment for a VM provisioner and return the provisioner instance.
 */
suspend fun createProvisioner(
    root: Domain,
    environmentReader: EnvironmentReader,
    allocationPolicy: AllocationPolicy
): Pair<ProvisioningService, SimpleVirtProvisioningService> = withContext(root.coroutineContext) {
    val environment = environmentReader.use { it.construct(root) }
    val bareMetalProvisioner = environment.platforms[0].zones[0].services[ProvisioningService.Key]

    // Wait for the bare metal nodes to be spawned
    delay(10)

    val scheduler = SimpleVirtProvisioningService(allocationPolicy, simulationContext, bareMetalProvisioner)

    // Wait for the hypervisors to be spawned
    delay(10)

    bareMetalProvisioner to scheduler
}

/**
 * Attach the specified monitor to the VM provisioner.
 */
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun attachMonitor(scheduler: SimpleVirtProvisioningService, reporter: Sc20Reporter) {
    val domain = simulationContext.domain
    val hypervisors = scheduler.drivers()

    // Monitor hypervisor events
    for (hypervisor in hypervisors) {
        // TODO Do not expose VirtDriver directly but use Hypervisor class.
        reporter.reportHostStateChange(hypervisor, (hypervisor as SimpleVirtDriver).server, scheduler.submittedVms, scheduler.queuedVms, scheduler.runningVms, scheduler.finishedVms)
        hypervisor.server.events
            .onEach { event ->
                when (event) {
                    is ServerEvent.StateChanged -> {
                        reporter.reportHostStateChange(hypervisor, event.server, scheduler.submittedVms, scheduler.queuedVms, scheduler.runningVms, scheduler.finishedVms)
                    }
                }
            }
            .launchIn(domain)
        hypervisor.events
            .onEach { event ->
                when (event) {
                    is HypervisorEvent.SliceFinished -> reporter.reportHostSlice(
                        simulationContext.clock.millis(),
                        event.requestedBurst,
                        event.grantedBurst,
                        event.overcommissionedBurst,
                        event.interferedBurst,
                        event.cpuUsage,
                        event.cpuDemand,
                        event.numberOfDeployedImages,
                        event.hostServer,
                        scheduler.submittedVms,
                        scheduler.queuedVms,
                        scheduler.runningVms,
                        scheduler.finishedVms
                    )
                }
            }
            .launchIn(domain)
    }
}

/**
 * Process the trace.
 */
suspend fun processTrace(reader: TraceReader<VmWorkload>, scheduler: SimpleVirtProvisioningService, chan: Channel<Unit>, reporter: Sc20Reporter, vmPlacements: Map<String, String> = emptyMap()) {
    val domain = simulationContext.domain

    try {
        var submitted = 0L
        val finished = Channel<Unit>(Channel.CONFLATED)
        val hypervisors = TreeSet(scheduler.drivers().map { (it as SimpleVirtDriver).server.name })

        while (reader.hasNext()) {
            val (time, workload) = reader.next()

            if (vmPlacements.isNotEmpty()) {
                val vmId = workload.name.replace("VM Workload ", "")
                // Check if VM in topology
                val clusterName = vmPlacements[vmId]
                if (clusterName == null) {
                    logger.warn { "Could not find placement data in VM placement file for VM $vmId" }
                    continue
                }
                val machineInCluster = hypervisors.ceiling(clusterName)?.contains(clusterName) ?: false
                if (machineInCluster) {
                    logger.info { "Ignored VM $vmId" }
                    continue
                }
            }

            submitted++
            delay(max(0, time - simulationContext.clock.millis()))
            domain.launch {
                chan.send(Unit)
                val server = scheduler.deploy(
                    workload.image.name, workload.image,
                    Flavor(workload.image.maxCores, workload.image.requiredMemory)
                )
                // Monitor server events
                server.events
                    .onEach {
                        if (it is ServerEvent.StateChanged) {
                            reporter.reportVmStateChange(it.server)
                        }

                        delay(1)
                        finished.send(Unit)
                    }
                    .collect()
            }
        }

        while (scheduler.finishedVms + scheduler.unscheduledVms != submitted) {
            finished.receive()
        }
    } finally {
        reader.close()
    }
}

/**
 * Main entry point of the experiment.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun main(args: Array<String>) {
    val cli = ArgParser(args).parseInto(::ExperimentParameters)
    logger.info("trace-directory: ${cli.traceDirectory}")
    logger.info("environment-file: ${cli.environmentFile}")
    logger.info("performance-interference-file: ${cli.performanceInterferenceFile}")
    logger.info("selected-vms-file: ${cli.selectedVmsFile}")
    logger.info("seed: ${cli.seed}")
    logger.info("failures: ${cli.failures}")
    logger.info("allocation-policy: ${cli.allocationPolicy}")

    val start = System.currentTimeMillis()
    val reporter: Sc20Reporter = Sc20ParquetReporter(cli.outputFile)

    val provider = ServiceLoader.load(SimulationEngineProvider::class.java).first()
    val system = provider("test")
    val root = system.newDomain("root")

    val chan = Channel<Unit>(Channel.CONFLATED)

    val performanceInterferenceModel = try {
        val performanceInterferenceStream = if (cli.performanceInterferenceFile != null) {
            File(cli.performanceInterferenceFile!!).inputStream().buffered()
        } else {
            object {}.javaClass.getResourceAsStream("/env/performance-interference.json")
        }
        Sc20PerformanceInterferenceReader(performanceInterferenceStream)
            .construct()
    } catch (e: Throwable) {
        reporter.close()
        throw e
    }
    val vmPlacements = if (cli.vmPlacementFile == null) {
        emptyMap()
    } else {
        Sc20VmPlacementReader(File(cli.vmPlacementFile!!).inputStream().buffered()).construct()
    }
    val environmentReader = Sc20ClusterEnvironmentReader(File(cli.environmentFile))
    val traceReader = try {
        createTraceReader(File(cli.traceDirectory), performanceInterferenceModel, cli.getSelectedVmList(), cli.seed)
    } catch (e: Throwable) {
        reporter.close()
        throw e
    }
    val allocationPolicy = when (cli.allocationPolicy) {
        "mem" -> AvailableMemoryAllocationPolicy()
        "mem-inv" -> AvailableMemoryAllocationPolicy(true)
        "core-mem" -> AvailableCoreMemoryAllocationPolicy()
        "core-mem-inv" -> AvailableCoreMemoryAllocationPolicy(true)
        "active-servers" -> NumberOfActiveServersAllocationPolicy()
        "active-servers-inv" -> NumberOfActiveServersAllocationPolicy(true)
        "provisioned-cores" -> ProvisionedCoresAllocationPolicy()
        "provisioned-cores-inv" -> ProvisionedCoresAllocationPolicy(true)
        "random" -> RandomAllocationPolicy(Random(cli.seed))
        "replay" -> ReplayAllocationPolicy(vmPlacements)
        else -> throw IllegalArgumentException("Unknown allocation policy: ${cli.allocationPolicy}")
    }

    root.launch {
        val (bareMetalProvisioner, scheduler) = createProvisioner(root, environmentReader, allocationPolicy)

        val failureDomain = if (cli.failures) {
            logger.info("ENABLING failures")
            createFailureDomain(cli.seed, cli.failureInterval, bareMetalProvisioner, chan)
        } else {
            null
        }

        attachMonitor(scheduler, reporter)
        processTrace(traceReader, scheduler, chan, reporter, vmPlacements)

        logger.debug("SUBMIT=${scheduler.submittedVms}")
        logger.debug("FAIL=${scheduler.unscheduledVms}")
        logger.debug("QUEUED=${scheduler.queuedVms}")
        logger.debug("RUNNING=${scheduler.runningVms}")
        logger.debug("FINISHED=${scheduler.finishedVms}")

        failureDomain?.cancel()
        scheduler.terminate()
        logger.info("Simulation took ${System.currentTimeMillis() - start} milliseconds")
    }

    runBlocking {
        system.run()
        system.terminate()
    }

    // Explicitly close the monitor to flush its buffer
    reporter.close()
}
