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

package org.opendc.compute.simulator.infrastructure

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.compute.simulator.service.SimTask
import org.opendc.simulator.compute.machine.SimMachine
import org.opendc.simulator.compute.models.CpuModel
import org.opendc.simulator.compute.models.MachineModel
import org.opendc.simulator.compute.models.MemoryUnit
import org.opendc.simulator.compute.power.PowerModel
import org.opendc.simulator.engine.engine.FlowEngine
import org.opendc.simulator.engine.graph.FlowDistributor
import org.opendc.simulator.engine.graph.distributionPolicies.FlowDistributorFactory
import java.time.InstantSource

class SimHostMemoryTest {
    @Test
    fun testUsedMemoryByRunningTasks() {
        val clock = InstantSource.fixed(java.time.Instant.EPOCH)
        val engine = mockk<FlowEngine>(relaxed = true)
        val cpuModel = CpuModel(0, 4, 2600.0, "vendor", "model", "arch")
        val memoryUnit = MemoryUnit("vendor", "model", 3200.0, 1024) // 1024 MB memory
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
        val simMachine = mockk<SimMachine>(relaxed = true)
        every { simMachine.canFit(any()) } returns true

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

        // SimHost creates its own SimMachine on construction; replace it with the mock through reflection.
        val simMachineField = host.javaClass.getDeclaredField("simMachine")
        simMachineField.isAccessible = true
        simMachineField.set(host, simMachine)

        val task1 = mockk<SimTask>(relaxed = true)
        every { task1.memorySize } returns 512
        every { task1.cpuCoreCount } returns 1

        val task2 = mockk<SimTask>(relaxed = true)
        every { task2.memorySize } returns 512
        every { task2.cpuCoreCount } returns 1

        val task3 = mockk<SimTask>(relaxed = true)
        every { task3.memorySize } returns 256
        every { task3.cpuCoreCount } returns 1

        // Initially can fit task1 and task2 (512 + 512 = 1024)
        assertTrue(host.canFit(task1), "Task 1 should fit initially")

        // After task1 is placed, used memory is 512. host capacity is 1024.
        // canFit(task2) should be true (1024 - 512 >= 512)
        host.reserve(task1)
        assertTrue(host.canFit(task2), "Task 2 should fit when Task 1 is running")

        // After task1 and task2 are placed, used memory is 1024.
        // canFit(task3) should be false (1024 - 1024 < 256)
        host.reserve(task2)
        assertFalse(host.canFit(task3), "Task 3 should not fit when Task 1 and 2 are running")

        // If task2 stops, its memory is released
        host.release(task2)
        assertTrue(host.canFit(task3), "Task 3 should fit after Task 2 stops running")

        // If only task3 runs, memory is 256
        // canFit task1 (512): (1024 - 256) >= 512 -> 768 >= 512 (true)
        host.release(task1)
        host.reserve(task3)
        assertTrue(host.canFit(task1), "Task 1 should fit with only task 3 running")
    }
}
