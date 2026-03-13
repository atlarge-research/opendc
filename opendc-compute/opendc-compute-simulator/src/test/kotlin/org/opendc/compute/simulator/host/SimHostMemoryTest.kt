package org.opendc.compute.simulator.host

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.compute.api.TaskState
import org.opendc.compute.simulator.service.ServiceTask
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
        val machineModel = MachineModel(cpuModel, memoryUnit, null, FlowDistributorFactory.DistributionPolicy.MAX_MIN_FAIRNESS, FlowDistributorFactory.DistributionPolicy.MAX_MIN_FAIRNESS)
        val powerModel = mockk<PowerModel>(relaxed = true)
        val distributor = mockk<FlowDistributor>(relaxed = true)
        val simMachine = mockk<SimMachine>(relaxed = true)
        every { simMachine.canFit(any()) } returns true

        val host = SimHost(
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
            powerDistributor = distributor
        )

        // Use reflection to set simMachine if needed, but SimHost.launch() sets it.
        // Actually SimHost has it as private var simMachine: SimMachine? = null
        // Let's try to trigger launch or just use reflection for testing private state.
        val simMachineField = host.javaClass.getDeclaredField("simMachine")
        simMachineField.isAccessible = true
        simMachineField.set(host, simMachine)

        val task1 = mockk<ServiceTask>(relaxed = true)
        every { task1.memorySize } returns 512L
        every { task1.cpuCoreCount } returns 1

        val task2 = mockk<ServiceTask>(relaxed = true)
        every { task2.memorySize } returns 512L
        every { task2.cpuCoreCount } returns 1

        val task3 = mockk<ServiceTask>(relaxed = true)
        every { task3.memorySize } returns 256L
        every { task3.cpuCoreCount } returns 1

        // Initially can fit task1 and task2 (512 + 512 = 1024)
        assertTrue(host.canFit(task1), "Task 1 should fit initially")

        // Mock taskToGuestMap to simulate running tasks
        val taskToGuestMapField = host.javaClass.getDeclaredField("taskToGuestMap")
        taskToGuestMapField.isAccessible = true
        val taskToGuestMap = taskToGuestMapField.get(host) as MutableMap<ServiceTask, Any>

        val guest1 = mockk<org.opendc.compute.simulator.internal.Guest>(relaxed = true)
        every { guest1.state } returns TaskState.RUNNING

        taskToGuestMap[task1] = guest1

        // After task1 is RUNNING, used memory is 512. host capacity is 1024.
        // canFit(task2) should be true (1024 - 512 >= 512)
        assertTrue(host.canFit(task2), "Task 2 should fit when Task 1 is running")

        val guest2 = mockk<org.opendc.compute.simulator.internal.Guest>(relaxed = true)
        every { guest2.state } returns TaskState.RUNNING
        taskToGuestMap[task2] = guest2

        // After task1 and task2 are RUNNING, used memory is 1024.
        // canFit(task3) should be false (1024 - 1024 < 256)
        assertFalse(host.canFit(task3), "Task 3 should not fit when Task 1 and 2 are running")

        // If guest2 stops
        every { guest2.state } returns TaskState.COMPLETED
        assertTrue(host.canFit(task3), "Task 3 should fit after Task 2 stops running")

        // If guest2 fails
        every { guest2.state } returns TaskState.FAILED
        assertTrue(host.canFit(task3), "Task 3 should fit after Task 2 fails")

        // If guest1 is paused
        every { guest1.state } returns TaskState.PAUSED
        assertTrue(host.canFit(task1), "Task 1 should fit when only task 1 is on host and it's paused")
        // But task1 is in taskToGuestMap. Memory calculation should not include it if it's not RUNNING.
        // Wait, if task1 is PAUSED, usedMemoryByRunningTasks() will sum 0 for it.
        // So host.canFit(task1) should return (1024 - 0) >= 512, which is true. Correct.

        // Add a running task3
        val guest3 = mockk<org.opendc.compute.simulator.internal.Guest>(relaxed = true)
        every { guest3.state } returns TaskState.RUNNING
        taskToGuestMap[task3] = guest3
        // Memory: task1 (PAUSED, 0) + task2 (FAILED, 0) + task3 (RUNNING, 256) = 256
        // canFit task1 (512): (1024 - 256) >= 512 -> 768 >= 512 (true)
        assertTrue(host.canFit(task1), "Task 1 should fit with only task 3 running")
    }
}
