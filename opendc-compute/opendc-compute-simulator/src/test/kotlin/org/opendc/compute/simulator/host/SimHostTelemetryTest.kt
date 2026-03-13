/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.compute.simulator.host

import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Test
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.simulator.compute.models.CpuModel
import org.opendc.simulator.compute.models.MachineModel
import org.opendc.simulator.compute.models.MemoryUnit
import org.opendc.simulator.compute.power.PowerModel
import org.opendc.simulator.engine.engine.FlowEngine
import org.opendc.simulator.engine.graph.FlowDistributor
import org.opendc.simulator.engine.graph.distributionPolicies.FlowDistributorFactory
import java.time.InstantSource

class SimHostTelemetryTest {
    @Test
    fun testGetCpuStatsForMissingTask() {
        val clock = InstantSource.fixed(java.time.Instant.EPOCH)
        val engine = mockk<FlowEngine>(relaxed = true)
        val cpuModel = CpuModel(0, 4, 2600.0, "vendor", "model", "arch")
        val memoryUnit = MemoryUnit("vendor", "model", 3200.0, 1024)
        val machineModel =
            MachineModel(
                cpuModel,
                memoryUnit,
                null,
                FlowDistributorFactory.DistributionPolicy.MAX_MIN_FAIRNESS,
                FlowDistributorFactory.DistributionPolicy.MAX_MIN_FAIRNESS,
            )
        val powerModel = mockk<PowerModel>(relaxed = true)
        val distributor = mockk<FlowDistributor>(relaxed = true)

        val host =
            SimHost(
                name = "H01",
                type = "host",
                clusterName = "C01",
                clock = clock,
                engine = engine,
                machineModel = machineModel,
                cpuPowerModel = powerModel,
                gpuPowerModel = null,
                embodiedCarbon = 0.0,
                expectedLifetime = 0.0,
                powerDistributor = distributor,
            )

        val task = mockk<ServiceTask>(relaxed = true)
        io.mockk.every { task.name } returns "T01"

        // This should not throw even if the task is not on the host
        assertDoesNotThrow {
            host.getCpuStats(task)
        }
    }
}
