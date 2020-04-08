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
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerEvent
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.metal.NODE_CLUSTER
import com.atlarge.opendc.compute.metal.service.ProvisioningService
import com.atlarge.opendc.compute.virt.HypervisorEvent
import com.atlarge.opendc.compute.virt.driver.SimpleVirtDriver
import com.atlarge.opendc.compute.virt.service.SimpleVirtProvisioningService
import com.atlarge.opendc.compute.virt.service.allocation.AvailableCoreMemoryAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.AvailableMemoryAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.NumberOfActiveServersAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.ProvisionedCoresAllocationPolicy
import com.atlarge.opendc.compute.virt.service.allocation.RandomAllocationPolicy
import com.atlarge.opendc.core.failure.CorrelatedFaultInjector
import com.atlarge.opendc.core.failure.FailureDomain
import com.atlarge.opendc.core.failure.FaultInjector
import com.atlarge.opendc.format.environment.sc20.Sc20ClusterEnvironmentReader
import com.atlarge.opendc.format.trace.sc20.Sc20PerformanceInterferenceReader
import com.atlarge.opendc.format.trace.sc20.Sc20TraceReader
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
import java.io.File
import java.io.FileReader
import java.util.ServiceLoader
import kotlin.math.max
import kotlin.random.Random

class ExperimentParameters(parser: ArgParser) {
    val traceDirectory by parser.storing("path to the trace directory")
    val environmentFile by parser.storing("path to the environment file")
    val performanceInterferenceFile by parser.storing("path to the performance interference file").default { null }
    val outputFile by parser.storing("path to where the output should be stored")
        .default { "sc20-experiment-results.csv" }
    val selectedVms by parser.storing("the VMs to run") { parseVMs(this) }
        .default { emptyList() }
    val selectedVmsFile by parser.storing("path to a file containing the VMs to run") {
        parseVMs(FileReader(File(this)).readText())
    }
        .default { emptyList() }
    val failures by parser.flagging("-x", "--failures", help = "enable (correlated) machine failures")
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
 * Obtain the [FaultInjector] to use for the experiments.
 */
fun createFaultInjector(domain: Domain, random: Random): FaultInjector {
    // Parameters from A. Iosup, A Framework for the Study of Grid Inter-Operation Mechanisms, 2009
    // GRID'5000
    return CorrelatedFaultInjector(domain,
        iatScale = -1.39, iatShape = 1.03, // Hours
        sizeScale = 1.88, sizeShape = 1.25,
        dScale = 9.51, dShape = 3.21, // Minutes
        random = random
    )
}

/**
 * Main entry point of the experiment.
 */
@OptIn(ExperimentalCoroutinesApi::class)
fun main(args: Array<String>) {
    ArgParser(args).parseInto(::ExperimentParameters).run {
        val start = System.currentTimeMillis()
        val monitor = Sc20Monitor(outputFile)

        val provider = ServiceLoader.load(SimulationEngineProvider::class.java).first()
        val system = provider("test")
        val root = system.newDomain("root")
        val chan = Channel<Unit>(Channel.CONFLATED)

        val allocationPolicies = mapOf(
            "mem" to AvailableMemoryAllocationPolicy(),
            "mem-inv" to AvailableMemoryAllocationPolicy(true),
            "core-mem" to AvailableCoreMemoryAllocationPolicy(),
            "core-mem-inv" to AvailableCoreMemoryAllocationPolicy(true),
            "active-servers" to NumberOfActiveServersAllocationPolicy(),
            "active-servers-inv" to NumberOfActiveServersAllocationPolicy(true),
            "provisioned-cores" to ProvisionedCoresAllocationPolicy(),
            "provisioned-cores-inv" to ProvisionedCoresAllocationPolicy(true),
            "random" to RandomAllocationPolicy()
        )

        if (allocationPolicy !in allocationPolicies) {
            println("error: unknown allocation policy $allocationPolicy")
            println("Available:")
            allocationPolicies.keys.forEach { key -> println(key) }
        }

        root.launch {
            val environment = Sc20ClusterEnvironmentReader(File(environmentFile))
                .use { it.construct(root) }

            val performanceInterferenceStream = if (performanceInterferenceFile != null) {
                File(performanceInterferenceFile!!).inputStream().buffered()
            } else {
                object {}.javaClass.getResourceAsStream("/env/performance-interference.json")
            }

            val performanceInterferenceModel = Sc20PerformanceInterferenceReader(performanceInterferenceStream)
                .construct()

            println(simulationContext.clock.instant())

            val bareMetalProvisioner = environment.platforms[0].zones[0].services[ProvisioningService.Key]

            // Wait for the bare metal nodes to be spawned
            delay(10)

            val scheduler = SimpleVirtProvisioningService(
                allocationPolicies.getValue(allocationPolicy),
                simulationContext,
                bareMetalProvisioner
            )

            // Wait for the hypervisors to be spawned
            delay(10)

            val hypervisors = scheduler.drivers()
            var availableHypervisors = hypervisors.size

            // Monitor hypervisor events
            for (hypervisor in hypervisors) {
                // TODO Do not expose VirtDriver directly but use Hypervisor class.
                monitor.serverStateChanged(hypervisor, (hypervisor as SimpleVirtDriver).server)
                hypervisor.server.events
                    .onEach { event ->
                        when (event) {
                            is ServerEvent.StateChanged -> {
                                monitor.serverStateChanged(hypervisor, event.server)

                                if (event.server.state == ServerState.ERROR)
                                    availableHypervisors -= 1
                            }
                        }
                    }
                    .launchIn(this)
                hypervisor.events
                    .onEach { event ->
                        when (event) {
                            is HypervisorEvent.SliceFinished -> monitor.onSliceFinish(
                                simulationContext.clock.millis(),
                                event.requestedBurst,
                                event.grantedBurst,
                                event.overcommissionedBurst,
                                event.interferredBurst,
                                event.numberOfDeployedImages,
                                event.hostServer
                            )
                        }
                    }
                    .launchIn(this)
            }

            val failureDomain = if (failures) {
                println("ENABLING failures")
                val domain = root.newDomain(name = "failures")
                domain.launch {
                    chan.receive()
                    val random = Random(0)
                    val injectors = mutableMapOf<String, FaultInjector>()
                    for (node in bareMetalProvisioner.nodes()) {
                        val cluster = node.metadata[NODE_CLUSTER] as String
                        val injector = injectors.getOrPut(cluster) { createFaultInjector(simulationContext.domain, random) }
                        injector.enqueue(node.metadata["driver"] as FailureDomain)
                    }
                }
                domain
            } else {
                null
            }

            val running = mutableSetOf<Server>()
            val finish = Channel<Unit>(Channel.RENDEZVOUS)

            val reader = Sc20TraceReader(File(traceDirectory), performanceInterferenceModel, getSelectedVmList())
            while (reader.hasNext()) {
                val (time, workload) = reader.next()
                delay(max(0, time - simulationContext.clock.millis()))
                launch {
                    chan.send(Unit)
                    val server = scheduler.deploy(
                        workload.image.name, workload.image,
                        Flavor(workload.image.maxCores, workload.image.requiredMemory)
                    )
                    running += server
                    // Monitor server events
                    server.events
                        .onEach {
                            if (it is ServerEvent.StateChanged)
                                monitor.onVmStateChanged(it.server)

                            // Detect whether the VM has finished running
                            if (it.server.state == ServerState.SHUTOFF) {
                                running -= server
                            }

                            if (running.isEmpty() && !reader.hasNext()) {
                                finish.send(Unit)
                            }
                        }
                        .collect()
                }
            }

            finish.receive()
            scheduler.terminate()
            failureDomain?.cancel()
            println(simulationContext.clock.instant())
            println("${System.currentTimeMillis() - start} milliseconds")
        }

        runBlocking {
            system.run()
            system.terminate()
        }

        // Explicitly close the monitor to flush its buffer
        monitor.close()
    }
}
