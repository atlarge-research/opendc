/*
 * Copyright (c) 2024 AtLarge Research
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

package org.opendc.simulator.compute

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.NetworkAdapter
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.model.StorageDevice
import org.opendc.simulator.compute.power.CpuPowerModels
import org.opendc.simulator.compute.workload.SimWorkloads
import org.opendc.simulator.flow2.FlowEngine
import org.opendc.simulator.kotlin.runSimulation
import org.opendc.simulator.power.SimPowerSource

class ThermalTest {
    private val maxPower = 130.0
    private val idlePower = 12.0

    private val powerModel = CpuPowerModels.linear(maxPower, idlePower)
    private lateinit var machineModel: MachineModel

    @BeforeEach
    fun setUp() {
        val cpuNode = ProcessingNode("Intel", "i7-900", "amd64", 8)

        machineModel =
            MachineModel(
                List(cpuNode.coreCount) { ProcessingUnit(cpuNode, it, 3500.0) },
                List(4) { MemoryUnit("Crucial", "MTA18ASF4G72AZ-3G2B1", 3200.0, 32_000) },
                listOf(NetworkAdapter("Mellanox", "ConnectX-5", 25000.0)),
                listOf(StorageDevice("Samsung", "EVO", 1000.0, 250.0, 250.0)),
            )
    }

    private fun testUtilization(
        utilization: Double,
        expectedPower: Double,
        expectedTemperature: Double,
    ) = runSimulation {
        val engine = FlowEngine.create(dispatcher)
        val graph = engine.newGraph()
        val machine =
            SimBareMetalMachine.create(
                graph,
                machineModel,
                SimPsuFactories.simple(powerModel),
            )
        val source = SimPowerSource(graph, 1000.0f)
        source.connect(machine.psu)

        coroutineScope {
            launch { machine.runWorkload(SimWorkloads.flops(2_000, utilization)) }

            yield()
            assertAll({
                assertEquals(expectedPower, machine.psu.thermalPower, 0.0001, "The power draw should be $expectedPower W")
                assertEquals(expectedTemperature, machine.psu.cpuTemperature, 0.0001, "The temperature should be $expectedTemperature C")
            })
        }
    }

    @Test
    fun zeroUtilization() = testUtilization(0.01, 24.0012, 45.48)

    @Test
    fun quarterUtilization() = testUtilization(0.25, 53.5012, 51.085)

    @Test
    fun halfUtilization() = testUtilization(0.50, 83.0012, 56.69)

    @Test
    fun threeQuarterUtilization() = testUtilization(0.75, 112.5012, 62.295)

    @Test
    fun fullUtilization() = testUtilization(1.0, 142.0012, 67.9)
}
