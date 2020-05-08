/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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
import com.atlarge.odcsim.SimulationEngine
import com.atlarge.odcsim.SimulationEngineProvider
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.workload.VmWorkload
import com.atlarge.opendc.compute.virt.service.SimpleVirtProvisioningService
import com.atlarge.opendc.compute.virt.service.allocation.AvailableCoreMemoryAllocationPolicy
import com.atlarge.opendc.format.environment.EnvironmentReader
import com.atlarge.opendc.format.environment.sc20.Sc20ClusterEnvironmentReader
import com.atlarge.opendc.format.trace.TraceReader
import com.atlarge.opendc.format.trace.sc20.Sc20PerformanceInterferenceReader
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.util.ServiceLoader

/**
 * An integration test suite for the SC20 experiments.
 */
class Sc20IntegrationTest {
    /**
     * The simulation engine to use.
     */
    private lateinit var simulationEngine: SimulationEngine

    /**
     * The root simulation domain to run in.
     */
    private lateinit var root: Domain

    /**
     * The monitor used to keep track of the metrics.
     */
    private lateinit var monitor: TestSc20Reporter

    /**
     * Setup the experimental environment.
     */
    @BeforeEach
    fun setUp() {
        val provider = ServiceLoader.load(SimulationEngineProvider::class.java).first()
        simulationEngine = provider("test")
        root = simulationEngine.newDomain("root")
        monitor = TestSc20Reporter()
    }

    /**
     * Tear down the experimental environment.
     */
    @AfterEach
    fun tearDown() = runBlocking {
        simulationEngine.terminate()
    }

    @Test
    fun smoke() {
        val failures = false
        val seed = 0
        val chan = Channel<Unit>(Channel.CONFLATED)
        val allocationPolicy = AvailableCoreMemoryAllocationPolicy()
        val traceReader = createTestTraceReader()
        val environmentReader = createTestEnvironmentReader()
        lateinit var scheduler: SimpleVirtProvisioningService

        root.launch {
            val res = createProvisioner(root, environmentReader, allocationPolicy)
            val bareMetalProvisioner = res.first
            scheduler = res.second

            val failureDomain = if (failures) {
                println("ENABLING failures")
                createFailureDomain(seed, 24 * 7, bareMetalProvisioner, chan)
            } else {
                null
            }

            attachMonitor(scheduler, monitor)
            processTrace(traceReader, scheduler, chan, monitor)

            println("Finish SUBMIT=${scheduler.submittedVms} FAIL=${scheduler.unscheduledVms} QUEUE=${scheduler.queuedVms} RUNNING=${scheduler.runningVms} FINISH=${scheduler.finishedVms}")

            failureDomain?.cancel()
            scheduler.terminate()
        }

        runSimulation()

        // Note that these values have been verified beforehand
        assertEquals(50, scheduler.submittedVms, "The trace contains 50 VMs")
        assertEquals(50, scheduler.finishedVms, "All VMs should finish after a run")
        assertEquals(207379117949, monitor.totalRequestedBurst)
        assertEquals(207378478631, monitor.totalGrantedBurst)
        assertEquals(639360, monitor.totalOvercommissionedBurst)
        assertEquals(0, monitor.totalInterferedBurst)
    }

    /**
     * Run the simulation.
     */
    private fun runSimulation() = runBlocking {
        simulationEngine.run()
    }

    /**
     * Obtain the trace reader for the test.
     */
    private fun createTestTraceReader(): TraceReader<VmWorkload> {
        val performanceInterferenceStream = object {}.javaClass.getResourceAsStream("/env/performance-interference.json")
        val performanceInterferenceModel = Sc20PerformanceInterferenceReader(performanceInterferenceStream)
            .construct()
        return createTraceReader(File("src/test/resources/trace"), performanceInterferenceModel, emptyList(), 0)
    }

    /**
     * Obtain the environment reader for the test.
     */
    private fun createTestEnvironmentReader(): EnvironmentReader {
        val stream = object {}.javaClass.getResourceAsStream("/env/topology.txt")
        return Sc20ClusterEnvironmentReader(stream)
    }

    class TestSc20Reporter : Sc20Reporter {
        var totalRequestedBurst = 0L
        var totalGrantedBurst = 0L
        var totalOvercommissionedBurst = 0L
        var totalInterferedBurst = 0L

        override suspend fun reportHostSlice(
            time: Long,
            requestedBurst: Long,
            grantedBurst: Long,
            overcommissionedBurst: Long,
            interferedBurst: Long,
            cpuUsage: Double,
            cpuDemand: Double,
            numberOfDeployedImages: Int,
            hostServer: Server,
            submittedVms: Long,
            queuedVms: Long,
            runningVms: Long,
            finishedVms: Long,
            duration: Long
        ) {
            totalRequestedBurst += requestedBurst
            totalGrantedBurst += grantedBurst
            totalOvercommissionedBurst += overcommissionedBurst
            totalInterferedBurst += interferedBurst
        }
        override fun close() {}
    }
}
