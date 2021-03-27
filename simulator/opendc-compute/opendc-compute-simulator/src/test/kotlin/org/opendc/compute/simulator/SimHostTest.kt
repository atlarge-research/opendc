/*
 * Copyright (c) 2020 AtLarge Research
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

package org.opendc.compute.simulator

import io.opentelemetry.api.metrics.MeterProvider
import io.opentelemetry.sdk.common.CompletableResultCode
import io.opentelemetry.sdk.metrics.SdkMeterProvider
import io.opentelemetry.sdk.metrics.data.MetricData
import io.opentelemetry.sdk.metrics.export.MetricExporter
import io.opentelemetry.sdk.metrics.export.MetricProducer
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.opendc.compute.api.Flavor
import org.opendc.compute.api.Image
import org.opendc.compute.api.Server
import org.opendc.compute.api.ServerState
import org.opendc.compute.api.ServerWatcher
import org.opendc.compute.service.driver.Host
import org.opendc.compute.service.driver.HostListener
import org.opendc.simulator.compute.SimFairShareHypervisorProvider
import org.opendc.simulator.compute.SimMachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.workload.SimTraceWorkload
import org.opendc.simulator.utils.DelayControllerClockAdapter
import org.opendc.telemetry.sdk.metrics.export.CoroutineMetricReader
import org.opendc.telemetry.sdk.toOtelClock
import java.util.UUID
import kotlin.coroutines.resume

/**
 * Basic test-suite for the hypervisor.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SimHostTest {
    private lateinit var machineModel: SimMachineModel

    @BeforeEach
    fun setUp() {
        val cpuNode = ProcessingNode("Intel", "Xeon", "amd64", 2)

        machineModel = SimMachineModel(
            cpus = List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 3200.0) },
            memory = List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) }
        )
    }

    /**
     * Test overcommitting of resources by the hypervisor.
     */
    @Test
    fun testOvercommitted() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        var requestedWork = 0L
        var grantedWork = 0L
        var overcommittedWork = 0L

        val meterProvider: MeterProvider = SdkMeterProvider
            .builder()
            .setClock(clock.toOtelClock())
            .build()

        val virtDriver = SimHost(UUID.randomUUID(), "test", machineModel, emptyMap(), coroutineContext, clock, meterProvider.get("opendc-compute-simulator"), SimFairShareHypervisorProvider())
        val duration = 5 * 60L
        val vmImageA = MockImage(
            UUID.randomUUID(),
            "<unnamed>",
            emptyMap(),
            mapOf(
                "workload" to SimTraceWorkload(
                    sequenceOf(
                        SimTraceWorkload.Fragment(duration * 1000, 28.0, 2),
                        SimTraceWorkload.Fragment(duration * 1000, 3500.0, 2),
                        SimTraceWorkload.Fragment(duration * 1000, 0.0, 2),
                        SimTraceWorkload.Fragment(duration * 1000, 183.0, 2)
                    ),
                )
            )
        )
        val vmImageB = MockImage(
            UUID.randomUUID(),
            "<unnamed>",
            emptyMap(),
            mapOf(
                "workload" to SimTraceWorkload(
                    sequenceOf(
                        SimTraceWorkload.Fragment(duration * 1000, 28.0, 2),
                        SimTraceWorkload.Fragment(duration * 1000, 3100.0, 2),
                        SimTraceWorkload.Fragment(duration * 1000, 0.0, 2),
                        SimTraceWorkload.Fragment(duration * 1000, 73.0, 2)
                    )
                )
            )
        )

        val flavor = MockFlavor(2, 0)

        // Setup metric reader
        val reader = CoroutineMetricReader(
            this, listOf(meterProvider as MetricProducer),
            object : MetricExporter {
                override fun export(metrics: Collection<MetricData>): CompletableResultCode {
                    val metricsByName = metrics.associateBy { it.name }
                    requestedWork += metricsByName.getValue("cpu.work.total").doubleSummaryData.points.first().sum.toLong()
                    grantedWork += metricsByName.getValue("cpu.work.granted").doubleSummaryData.points.first().sum.toLong()
                    overcommittedWork += metricsByName.getValue("cpu.work.overcommit").doubleSummaryData.points.first().sum.toLong()
                    return CompletableResultCode.ofSuccess()
                }

                override fun flush(): CompletableResultCode = CompletableResultCode.ofSuccess()

                override fun shutdown(): CompletableResultCode = CompletableResultCode.ofSuccess()
            },
            exportInterval = duration * 1000
        )

        coroutineScope {
            launch { virtDriver.spawn(MockServer(UUID.randomUUID(), "a", flavor, vmImageA)) }
            launch { virtDriver.spawn(MockServer(UUID.randomUUID(), "b", flavor, vmImageB)) }

            suspendCancellableCoroutine<Unit> { cont ->
                virtDriver.addListener(object : HostListener {
                    private var finished = 0

                    override fun onStateChanged(host: Host, server: Server, newState: ServerState) {
                        if (newState == ServerState.TERMINATED && ++finished == 2) {
                            cont.resume(Unit)
                        }
                    }
                })
            }
        }

        // Ensure last cycle is collected
        delay(1000 * duration)
        virtDriver.close()
        reader.close()

        assertAll(
            { assertEquals(4197600, requestedWork, "Requested work does not match") },
            { assertEquals(2157600, grantedWork, "Granted work does not match") },
            { assertEquals(2040000, overcommittedWork, "Overcommitted work does not match") },
            { assertEquals(1500001, currentTime) }
        )
    }

    private class MockFlavor(
        override val cpuCount: Int,
        override val memorySize: Long
    ) : Flavor {
        override val uid: UUID = UUID.randomUUID()
        override val name: String = "test"
        override val labels: Map<String, String> = emptyMap()
        override val meta: Map<String, Any> = emptyMap()

        override suspend fun delete() {
            throw NotImplementedError()
        }

        override suspend fun refresh() {
            throw NotImplementedError()
        }
    }

    private class MockImage(
        override val uid: UUID,
        override val name: String,
        override val labels: Map<String, String>,
        override val meta: Map<String, Any>
    ) : Image {
        override suspend fun delete() {
            throw NotImplementedError()
        }

        override suspend fun refresh() {
            throw NotImplementedError()
        }
    }

    private class MockServer(
        override val uid: UUID,
        override val name: String,
        override val flavor: Flavor,
        override val image: Image
    ) : Server {
        override val labels: Map<String, String> = emptyMap()

        override val meta: Map<String, Any> = emptyMap()

        override val state: ServerState = ServerState.TERMINATED

        override suspend fun start() {}

        override suspend fun stop() {}

        override suspend fun delete() {}

        override fun watch(watcher: ServerWatcher) {}

        override fun unwatch(watcher: ServerWatcher) {}

        override suspend fun refresh() {}
    }
}
