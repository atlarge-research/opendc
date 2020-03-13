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

import com.atlarge.odcsim.SimulationEngineProvider
import com.atlarge.odcsim.simulationContext
import com.atlarge.opendc.compute.core.Flavor
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.metal.service.ProvisioningService
import com.atlarge.opendc.compute.virt.service.SimpleVirtProvisioningService
import com.atlarge.opendc.compute.virt.service.allocation.AvailableMemoryAllocationPolicy
import com.atlarge.opendc.format.environment.sc20.Sc20ClusterEnvironmentReader
import com.atlarge.opendc.format.trace.sc20.Sc20PerformanceInterferenceReader
import com.atlarge.opendc.format.trace.sc20.Sc20TraceReader
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.io.File
import java.util.ServiceLoader
import kotlin.math.max

class ExperimentParameters(parser: ArgParser) {
    val traceDirectory by parser.storing("path to the trace directory")
    val environmentFile by parser.storing("path to the environment file")
    val outputFile by parser.storing("path to where the output should be stored")
        .default { "sc20-experiment-results.csv" }
    val selectedVms by parser.storing("the VMs to run") {
            // Handle case where VM list contains a VM name with an (escaped) single-quote in it
            val string = this.replace("\\'", "\\\\[")
                .replace("'", "\"")
                .replace("\\\\[", "'")
            val vms: List<String> = jacksonObjectMapper().readValue(string)
            vms
        }
        .default {
            emptyList()
        }
}

/**
 * Main entry point of the experiment.
 */
fun main(args: Array<String>) {
    ArgParser(args).parseInto(::ExperimentParameters).run {
        val hypervisorMonitor = Sc20HypervisorMonitor(outputFile)
        val monitor = object : ServerMonitor {
            override suspend fun onUpdate(server: Server, previousState: ServerState) {
                println(server)
            }
        }

        val provider = ServiceLoader.load(SimulationEngineProvider::class.java).first()
        val system = provider("test")
        val root = system.newDomain("root")

        root.launch {
            val environment = Sc20ClusterEnvironmentReader(File(environmentFile))
                .use { it.construct(root) }

            val performanceInterferenceModel = Sc20PerformanceInterferenceReader(
                object {}.javaClass.getResourceAsStream("/env/performance-interference.json")
            ).construct()

            println(simulationContext.clock.instant())

            val scheduler = SimpleVirtProvisioningService(
                AvailableMemoryAllocationPolicy(),
                simulationContext,
                environment.platforms[0].zones[0].services[ProvisioningService.Key],
                hypervisorMonitor
            )

            val reader = Sc20TraceReader(File(traceDirectory), performanceInterferenceModel, selectedVms)
            while (reader.hasNext()) {
                val (time, workload) = reader.next()
                delay(max(0, time - simulationContext.clock.millis()))
                scheduler.deploy(workload.image, monitor, Flavor(workload.image.cores, workload.image.requiredMemory))
            }

            println(simulationContext.clock.instant())
        }

        runBlocking {
            system.run()
            system.terminate()
        }

        // Explicitly close the monitor to flush its buffer
        hypervisorMonitor.close()
    }
}
