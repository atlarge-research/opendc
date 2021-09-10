/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.experiments.capelin

import io.opentelemetry.api.metrics.MeterProvider
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import kotlinx.coroutines.*
import org.apache.commons.math3.distribution.LogNormalDistribution
import org.apache.commons.math3.random.Well19937c
import org.opendc.compute.api.*
import org.opendc.compute.service.ComputeService
import org.opendc.compute.service.scheduler.ComputeScheduler
import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.ReplayScheduler
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.filters.RamFilter
import org.opendc.compute.service.scheduler.filters.VCpuFilter
import org.opendc.compute.service.scheduler.weights.CoreRamWeigher
import org.opendc.compute.service.scheduler.weights.InstanceCountWeigher
import org.opendc.compute.service.scheduler.weights.RamWeigher
import org.opendc.compute.service.scheduler.weights.VCpuWeigher
import org.opendc.compute.simulator.SimHost
import org.opendc.compute.simulator.failure.HostFaultInjector
import org.opendc.compute.simulator.failure.StartStopHostFault
import org.opendc.compute.simulator.failure.StochasticVictimSelector
import org.opendc.experiments.capelin.env.EnvironmentReader
import org.opendc.experiments.capelin.trace.TraceReader
import org.opendc.simulator.compute.kernel.SimFairShareHypervisorProvider
import org.opendc.simulator.compute.kernel.interference.VmInterferenceModel
import org.opendc.simulator.compute.power.SimplePowerDriver
import org.opendc.simulator.compute.workload.SimTraceWorkload
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.resources.SimResourceInterpreter
import org.opendc.telemetry.compute.ComputeMonitor
import org.opendc.telemetry.sdk.toOtelClock
import java.time.Clock
import kotlin.coroutines.CoroutineContext
import kotlin.math.ln
import kotlin.math.max
import kotlin.random.Random

/**
 * Obtain the [FaultInjector] to use for the experiments.
 */
fun createFaultInjector(
    context: CoroutineContext,
    clock: Clock,
    hosts: Set<SimHost>,
    seed: Int,
    failureInterval: Double
): HostFaultInjector {
    val rng = Well19937c(seed)

    // Parameters from A. Iosup, A Framework for the Study of Grid Inter-Operation Mechanisms, 2009
    // GRID'5000
    return HostFaultInjector(
        context,
        clock,
        hosts,
        iat = LogNormalDistribution(rng, ln(failureInterval), 1.03),
        selector = StochasticVictimSelector(LogNormalDistribution(rng, 1.88, 1.25), Random(seed)),
        fault = StartStopHostFault(LogNormalDistribution(rng, 8.89, 2.71))
    )
}

/**
 * Construct the environment for a simulated compute service..
 */
suspend fun withComputeService(
    clock: Clock,
    meterProvider: MeterProvider,
    environmentReader: EnvironmentReader,
    scheduler: ComputeScheduler,
    interferenceModel: VmInterferenceModel? = null,
    block: suspend CoroutineScope.(ComputeService) -> Unit
): Unit = coroutineScope {
    val interpreter = SimResourceInterpreter(coroutineContext, clock)
    val hosts = environmentReader
        .use { it.read() }
        .map { def ->
            SimHost(
                def.uid,
                def.name,
                def.model,
                def.meta,
                coroutineContext,
                interpreter,
                meterProvider.get("opendc-compute-simulator"),
                SimFairShareHypervisorProvider(),
                powerDriver = SimplePowerDriver(def.powerModel),
                interferenceDomain = interferenceModel?.newDomain()
            )
        }

    val serviceMeter = meterProvider.get("opendc-compute")
    val service =
        ComputeService(coroutineContext, clock, serviceMeter, scheduler)

    for (host in hosts) {
        service.addHost(host)
    }

    try {
        block(this, service)
    } finally {
        service.close()
        hosts.forEach(SimHost::close)
    }
}

/**
 * Process the trace.
 */
suspend fun processTrace(
    clock: Clock,
    reader: TraceReader<SimWorkload>,
    scheduler: ComputeService,
    monitor: ComputeMonitor? = null,
) {
    val client = scheduler.newClient()
    val watcher = object : ServerWatcher {
        override fun onStateChanged(server: Server, newState: ServerState) {
            monitor?.onStateChange(clock.millis(), server, newState)
        }
    }

    // Create new image for the virtual machine
    val image = client.newImage("vm-image")

    try {
        coroutineScope {
            var offset = Long.MIN_VALUE

            while (reader.hasNext()) {
                val entry = reader.next()

                if (offset < 0) {
                    offset = entry.start - clock.millis()
                }

                // Make sure the trace entries are ordered by submission time
                assert(entry.start - offset >= 0) { "Invalid trace order" }
                delay(max(0, (entry.start - offset) - clock.millis()))

                launch {
                    val workloadOffset = -offset + 300001
                    val workload = SimTraceWorkload((entry.meta["workload"] as SimTraceWorkload).trace, workloadOffset)

                    val server = client.newServer(
                        entry.name,
                        image,
                        client.newFlavor(
                            entry.name,
                            entry.meta["cores"] as Int,
                            entry.meta["required-memory"] as Long
                        ),
                        meta = entry.meta + mapOf("workload" to workload)
                    )
                    server.watch(watcher)

                    // Wait for the server reach its end time
                    val endTime = entry.meta["end-time"] as Long
                    delay(endTime + workloadOffset - clock.millis() + 1)

                    // Delete the server after reaching the end-time of the virtual machine
                    server.delete()
                }
            }
        }

        yield()
    } finally {
        reader.close()
        client.close()
    }
}

/**
 * Create a [MeterProvider] instance for the experiment.
 */
fun createMeterProvider(clock: Clock): MeterProvider {
    return SdkMeterProvider
        .builder()
        .setClock(clock.toOtelClock())
        .build()
}

/**
 * Create a [ComputeScheduler] for the experiment.
 */
fun createComputeScheduler(allocationPolicy: String, seeder: Random, vmPlacements: Map<String, String> = emptyMap()): ComputeScheduler {
    val cpuAllocationRatio = 16.0
    val ramAllocationRatio = 1.5
    return when (allocationPolicy) {
        "mem" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(RamWeigher(multiplier = 1.0))
        )
        "mem-inv" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(RamWeigher(multiplier = -1.0))
        )
        "core-mem" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(CoreRamWeigher(multiplier = 1.0))
        )
        "core-mem-inv" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(CoreRamWeigher(multiplier = -1.0))
        )
        "active-servers" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(InstanceCountWeigher(multiplier = -1.0))
        )
        "active-servers-inv" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(InstanceCountWeigher(multiplier = 1.0))
        )
        "provisioned-cores" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(VCpuWeigher(cpuAllocationRatio, multiplier = 1.0))
        )
        "provisioned-cores-inv" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = listOf(VCpuWeigher(cpuAllocationRatio, multiplier = -1.0))
        )
        "random" -> FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(cpuAllocationRatio), RamFilter(ramAllocationRatio)),
            weighers = emptyList(),
            subsetSize = Int.MAX_VALUE,
            random = java.util.Random(seeder.nextLong())
        )
        "replay" -> ReplayScheduler(vmPlacements)
        else -> throw IllegalArgumentException("Unknown policy $allocationPolicy")
    }
}
