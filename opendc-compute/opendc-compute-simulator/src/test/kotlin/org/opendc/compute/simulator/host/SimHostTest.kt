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

package org.opendc.compute.simulator.host

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.compute.models.CpuModel
import org.opendc.simulator.compute.models.MemoryUnit
import org.opendc.simulator.compute.models.MachineModel
import org.opendc.simulator.engine.graph.FlowGraph
import org.opendc.simulator.compute.cpu.CpuPowerModels
import org.opendc.simulator.engine.engine.FlowEngine
import org.opendc.simulator.engine.graph.FlowDistributor
import java.time.Clock
import org.opendc.common.Dispatcher
import org.opendc.compute.api.TaskState
import org.opendc.compute.simulator.service.ComputeService
import org.opendc.compute.simulator.service.ServiceFlavor
import org.opendc.compute.simulator.service.ServiceTask
import org.opendc.simulator.compute.workload.SimWorkload
import org.opendc.simulator.compute.workload.Workload
import java.time.InstantSource
import java.util.UUID

/**
 * Test suite for the [SimHost].
 */
internal class SimHostTest {

    /**
     * Initialize a SimHost for testing purposes. The dispatcher and the FlowGraph are mocked.
     */
    private fun initSimHost(): SimHost {
        val cpuModel = CpuModel(1, 8, 3000.0, "TEST", "TEST", "x86")
        val memoryUnit = MemoryUnit("TEST", "TEST", 1000.0, 320000)
        val machineModel = MachineModel(cpuModel, memoryUnit)
        val dispatcher = mockk<Dispatcher>()
        every { dispatcher.getTimeSource() } returns InstantSource.system()
        every { dispatcher.schedule(any(), any()) } returns Unit
        val engine = FlowEngine.create(dispatcher)
        val engine2 = FlowEngine.create(dispatcher)
        val flowGraph = mockk<FlowGraph>()
        every { flowGraph.engine } returns engine
        every {flowGraph.addNode(any())} just Runs
        every {flowGraph.removeNode(any())} just Runs
        every {flowGraph.addEdge(any(), any())} returns null
        val flowGraph2 = FlowGraph(engine2)
        val clock = Clock.systemUTC()
        val cpuPowerModel = CpuPowerModels.constant(500.0)
        val flowDistributor = FlowDistributor(flowGraph2)
        val host = SimHost("test", "C1", clock, flowGraph, machineModel, cpuPowerModel, flowDistributor)
        return host
    }

    /**
     * Initialize Workload
     */
    private fun initVmWorkload(coreCount: Int = 1, memorySize: Long = 1024): ServiceTask {
        val computeService = mockk<ComputeService>()
        every {computeService.clock } returns InstantSource.system()
        val vmID = UUID.randomUUID()
        val vmName = "name_of_a_vm"
        val meta = mockk<Map<String, Int>>()
        val serviceFlavor = ServiceFlavor(computeService, vmID, vmName, coreCount, memorySize, meta)
        val workload = mockk<Workload>()
        val simWorkload = mockk<SimWorkload>()
        every { workload.startWorkload(any(), any()) } returns simWorkload
        val serviceTask = ServiceTask(computeService, vmID, vmName, serviceFlavor, workload, meta)
        return serviceTask
    }

    @Test
    fun testInitSimHost() {
        val host = initSimHost()
        assertEquals("C1", host.getClusterName())
        assertEquals("SimHost[uid=test,name=test,model=HostModel[cpuCapacity=24000.0, coreCount=8, memoryCapacity=320000]]", host.toString())
        assertEquals("test", host.getName())
        assertEquals("HostModel[cpuCapacity=24000.0, coreCount=8, memoryCapacity=320000]", host.getModel().toString())
        assertFalse(host.equals("TEST-TEST"))
    }

    @Test
    fun testGetState() {
        val host = initSimHost()
        assertEquals(HostState.UP, host.getState())
    }

    @Test
    fun testCloseState() {
        val host = initSimHost()
        host.close()
        assertEquals(HostState.DOWN, host.getState())
    }

    @Test
    fun testFail() {
        val host = initSimHost()
        val serviceTask = initVmWorkload()
        host.spawn(serviceTask)
        host.start(serviceTask)
        assertTrue(host.contains(serviceTask))
        val hostStats = host.getSystemStats()
        val vms = host.getGuests()
        val vm = vms[0]
        assertEquals(hostStats.guestsRunning, 1)
        host.fail()
        assertEquals(HostState.ERROR, host.getState())
        assertEquals(TaskState.FAILED, vm.state)
    }

    @Test
    fun testRecoverAfterFail() {
        val host = initSimHost()
        val serviceTask = initVmWorkload()
        host.spawn(serviceTask)
        host.start(serviceTask)
        assertTrue(host.contains(serviceTask))
        val hostStats = host.getSystemStats()
        val vms = host.getGuests()
        val vm = vms[0]
        assertEquals(hostStats.guestsRunning, 1)
        host.fail()
        assertEquals(HostState.ERROR, host.getState())
        assertEquals(TaskState.FAILED, vm.state)
        host.recover()
        assertEquals(HostState.UP, host.getState())
        val hostStatsAfterRecover = host.getSystemStats()
        assertEquals(hostStatsAfterRecover.guestsRunning, 0)
    }

    @Test
    fun testGetSystemStats() {
        val host = initSimHost()
        val hostStats = host.getSystemStats()
        assertEquals(hostStats.powerDraw, 0.0)
        assertEquals(hostStats.energyUsage, 0.0)
        assertEquals(hostStats.guestsTerminated, 0)
        assertEquals(hostStats.guestsRunning, 0)
        assertEquals(hostStats.guestsError, 0)
        assertEquals(hostStats.guestsInvalid, 0)
    }

    @Test
    fun testFitVmWorkloadOnSimHostWorkloadFits() {
        val host = initSimHost()
        val serviceTask = initVmWorkload()
        val fit = host.canFit(serviceTask)
        assertTrue(fit)
    }

    @Test
    fun testSpawnVmWorkload() {
        val host = initSimHost()
        val serviceTask = initVmWorkload()
        host.spawn(serviceTask)
        assertTrue(host.contains(serviceTask))
        val hostStats = host.getSystemStats()
        assertEquals(hostStats.guestsRunning, 1)
    }

    @Test
    fun testFitVmWorkloadOnSimHostWorkloadDoesNotFit() {
        val host = initSimHost()
        val serviceTask = initVmWorkload(12)
        val fit = host.canFit(serviceTask)
        assertFalse(fit)
    }

    @Test
    fun testFitVmWorkloadOnSimHostSecondWorkloadDoesNotFit() {
        val host = initSimHost()
        val serviceTask1 = initVmWorkload()
        val serviceTask2 = initVmWorkload(8, 320000)
        assertTrue(host.canFit(serviceTask1))
        assertTrue(host.canFit(serviceTask2))
        host.spawn(serviceTask1)
        host.start(serviceTask1)
        assertFalse(host.canFit(serviceTask2))
        val exception = assertThrows<java.lang.IllegalArgumentException> {
            host.spawn(serviceTask2)
        }
        assertEquals(exception.message, "Task does not fit")
    }

    @Test
    fun testHostCpuStats() {
        val host = initSimHost()
        val serviceTask = initVmWorkload()
        host.spawn(serviceTask)
        host.start(serviceTask)
        assertTrue(host.contains(serviceTask))
        val hostStats = host.getSystemStats()
        assertEquals(hostStats.guestsRunning, 1)
        val cpuStats = host.getCpuStats()
        assertEquals(24000.0, cpuStats.capacity)
        assertEquals(0, cpuStats.activeTime)
        assertEquals(0, cpuStats.stealTime)
        assertEquals(0, cpuStats.lostTime)
        assertEquals(0.0, cpuStats.demand)
        assertEquals(0.0, cpuStats.usage)
        assertEquals(0.0, cpuStats.utilization)
        assertTrue(cpuStats.idleTime > 1)
    }

    @Test
    fun testRemoveTask() {
        val host = initSimHost()
        val serviceTask = initVmWorkload()
        host.spawn(serviceTask)
        host.start(serviceTask)
        assertTrue(host.contains(serviceTask))
        var hostStats = host.getSystemStats()
        assertEquals(hostStats.guestsRunning, 1)
        host.removeTask(serviceTask)
        hostStats = host.getSystemStats()
        assertEquals(hostStats.guestsRunning, 0)
    }

    @Test
    fun testStopTask() {
        val host = initSimHost()
        val serviceTask = initVmWorkload()
        host.spawn(serviceTask)
        host.start(serviceTask)
        assertTrue(host.contains(serviceTask))
        var hostStats = host.getSystemStats()
        val vms = host.getGuests()
        val vm = vms[0]
        assertEquals(hostStats.guestsRunning, 1)
        host.stop(serviceTask)
        hostStats = host.getSystemStats()
        assertEquals(hostStats.guestsRunning, 0)
        assertEquals(TaskState.COMPLETED, vm.state)
    }
}
