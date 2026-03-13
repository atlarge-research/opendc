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
        val machineModel = MachineModel(cpuModel, memoryUnit, null, FlowDistributorFactory.DistributionPolicy.MAX_MIN_FAIRNESS, FlowDistributorFactory.DistributionPolicy.MAX_MIN_FAIRNESS)
        val powerModel = mockk<PowerModel>(relaxed = true)
        val distributor = mockk<FlowDistributor>(relaxed = true)

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

        val task = mockk<ServiceTask>(relaxed = true)
        io.mockk.every { task.name } returns "T01"

        // This should not throw even if the task is not on the host
        assertDoesNotThrow {
            host.getCpuStats(task)
        }
    }
}
