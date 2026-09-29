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

package org.opendc.simulator.compute.scheduler

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.compute.infrastructure.SimHost
import org.opendc.simulator.compute.models.CpuModel
import org.opendc.simulator.compute.models.GpuHostModel
import org.opendc.simulator.compute.models.HostModel
import org.opendc.simulator.compute.models.HostState
import org.opendc.simulator.compute.models.MachineModel
import org.opendc.simulator.compute.models.MemoryUnit
import org.opendc.simulator.compute.power.PowerModel
import org.opendc.simulator.compute.scheduler.filters.ComputeFilter
import org.opendc.simulator.compute.scheduler.filters.DifferentHostFilter
import org.opendc.simulator.compute.scheduler.filters.HostFilter
import org.opendc.simulator.compute.scheduler.filters.InstanceCountFilter
import org.opendc.simulator.compute.scheduler.filters.RamFilter
import org.opendc.simulator.compute.scheduler.filters.SameHostFilter
import org.opendc.simulator.compute.scheduler.filters.VCpuCapacityFilter
import org.opendc.simulator.compute.scheduler.filters.VCpuFilter
import org.opendc.simulator.compute.scheduler.filters.VGpuCapacityFilter
import org.opendc.simulator.compute.scheduler.filters.VGpuFilter
import org.opendc.simulator.compute.scheduler.weights.CoreRamWeigher
import org.opendc.simulator.compute.scheduler.weights.HostWeigher
import org.opendc.simulator.compute.scheduler.weights.InstanceCountWeigher
import org.opendc.simulator.compute.scheduler.weights.RamWeigher
import org.opendc.simulator.compute.scheduler.weights.VCpuWeigher
import org.opendc.simulator.compute.task.SimTask
import org.opendc.simulator.flow.engine.FlowEngine
import org.opendc.simulator.flow.graph.FlowDistributor
import org.opendc.simulator.flow.graph.distributionPolicies.FlowDistributorFactory
import java.time.Instant
import java.time.InstantSource
import java.util.Random

/**
 * Test suite for the [FilterScheduler].
 */
internal class FilterSchedulerTest {
    @Test
    fun testNoHosts() {
        val scheduler =
            FilterScheduler(
                filters = emptyList(),
                weighers = emptyList(),
            )

        val req = mockk<SchedulingRequest>()
        every { req.task.cpuCoreCount } returns 2
        every { req.task.memorySize } returns 1024
        every { req.isCancelled } returns false

        assertEquals(SchedulingResultType.FAILURE, scheduler.select(mutableListOf(req).iterator()).resultType)
    }

    @Test
    fun testNoFiltersAndWeighters() {
        val scheduler =
            FilterScheduler(
                filters = emptyList(),
                weighers = emptyList(),
            )

        val hostA = mockk<SimHost>()
        every { hostA.state } returns HostState.DOWN
        every { hostA.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.isEmpty() } returns true

        val hostB = mockk<SimHost>()
        every { hostB.state } returns HostState.UP
        every { hostB.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.isEmpty() } returns true

        scheduler.addHost(hostA.withIds())
        scheduler.addHost(hostB.withIds())

        val req = mockk<SchedulingRequest>()
        every { req.task.cpuCoreCount } returns 2
        every { req.task.memorySize } returns 1024
        every { req.isCancelled } returns false

        // Make sure we get the first host both times
        assertAll(
            { assertEquals(hostA, scheduler.select(mutableListOf(req).iterator()).host) },
            { assertEquals(hostA, scheduler.select(mutableListOf(req).iterator()).host) },
        )
    }

    @Test
    fun testHostIsDown() {
        val scheduler =
            FilterScheduler(
                filters = listOf(ComputeFilter()),
                weighers = emptyList(),
            )

        val host = mockk<SimHost>()
        every { host.state } returns HostState.DOWN
        every { host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { host.isEmpty() } returns true

        scheduler.addHost(host.withIds())

        val req = mockk<SchedulingRequest>()
        every { req.task.cpuCoreCount } returns 2
        every { req.task.memorySize } returns 1024
        every { req.isCancelled } returns false

        assertEquals(SchedulingResultType.FAILURE, scheduler.select(mutableListOf(req).iterator()).resultType)
    }

    @Test
    fun testHostIsUp() {
        val scheduler =
            FilterScheduler(
                filters = listOf(ComputeFilter()),
                weighers = emptyList(),
            )

        val host = mockk<SimHost>()
        every { host.state } returns HostState.UP
        every { host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { host.isEmpty() } returns true

        scheduler.addHost(host.withIds())

        val req = mockk<SchedulingRequest>()
        every { req.task.cpuCoreCount } returns 2
        every { req.task.memorySize } returns 1024
        every { req.isCancelled } returns false

        assertEquals(host, scheduler.select(mutableListOf(req).iterator()).host)
    }

    @Test
    fun testRamFilter() {
        val scheduler =
            FilterScheduler(
                filters = listOf(RamFilter(1.0)),
                weighers = emptyList(),
            )

        val hostA = mockk<SimHost>()
        every { hostA.state } returns HostState.UP
        every { hostA.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.availableMemory } returns 512
        every { hostA.isEmpty() } returns false

        val hostB = mockk<SimHost>()
        every { hostB.state } returns HostState.UP
        every { hostB.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.availableMemory } returns 2048
        every { hostB.isEmpty() } returns true

        scheduler.addHost(hostA.withIds())
        scheduler.addHost(hostB.withIds())

        val req = mockk<SchedulingRequest>()
        every { req.task.cpuCoreCount } returns 2
        every { req.task.memorySize } returns 1024
        every { req.isCancelled } returns false

        assertEquals(hostB, scheduler.select(mutableListOf(req).iterator()).host)
    }

    @Test
    fun testRamFilterOvercommit() {
        val scheduler =
            FilterScheduler(
                filters = listOf(RamFilter(1.5)),
                weighers = emptyList(),
            )

        val host = mockk<SimHost>()
        every { host.state } returns HostState.UP
        every { host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { host.availableMemory } returns 2048
        every { host.isEmpty() } returns true

        scheduler.addHost(host.withIds())

        val req = mockk<SchedulingRequest>()
        every { req.task.cpuCoreCount } returns 2
        every { req.task.memorySize } returns 2300
        every { req.isCancelled } returns false

        assertEquals(SchedulingResultType.FAILURE, scheduler.select(mutableListOf(req).iterator()).resultType)
    }

    @Test
    fun testVCpuFilter() {
        val scheduler =
            FilterScheduler(
                filters = listOf(VCpuFilter(1.0)),
                weighers = emptyList(),
            )

        val hostA = mockk<SimHost>()
        every { hostA.state } returns HostState.UP
        every { hostA.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.provisionedCpuCores } returns 3
        every { hostA.availableCpuCores } returns 1
        every { hostA.isEmpty() } returns false

        val hostB = mockk<SimHost>()
        every { hostB.state } returns HostState.UP
        every { hostB.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.provisionedCpuCores } returns 0
        every { hostB.availableCpuCores } returns 4
        every { hostB.isEmpty() } returns true

        scheduler.addHost(hostA.withIds())
        scheduler.addHost(hostB.withIds())

        val req = mockk<SchedulingRequest>()
        every { req.task.cpuCoreCount } returns 2
        every { req.task.memorySize } returns 1024
        every { req.isCancelled } returns false

        assertEquals(hostB, scheduler.select(mutableListOf(req).iterator()).host)
    }

    @Test
    fun testVCpuFilterOvercommit() {
        val scheduler =
            FilterScheduler(
                filters = listOf(VCpuFilter(16.0)),
                weighers = emptyList(),
            )

        val host = mockk<SimHost>()
        every { host.state } returns HostState.UP
        every { host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { host.provisionedCpuCores } returns 0
        every { host.isEmpty() } returns true

        scheduler.addHost(host.withIds())

        val req = mockk<SchedulingRequest>()
        every { req.task.cpuCoreCount } returns 8
        every { req.task.memorySize } returns 1024
        every { req.isCancelled } returns false

        assertEquals(SchedulingResultType.FAILURE, scheduler.select(mutableListOf(req).iterator()).resultType)
    }

    @Test
    fun testVCpuCapacityFilter() {
        val scheduler =
            FilterScheduler(
                filters = listOf(VCpuCapacityFilter()),
                weighers = emptyList(),
            )

        val hostA = mockk<SimHost>()
        every { hostA.state } returns HostState.UP
        every { hostA.model } returns HostModel(8 * 2600.0, 8, 2048)
        every { hostA.availableMemory } returns 512
        every { hostA.isEmpty() } returns true
        scheduler.addHost(hostA.withIds())

        val hostB = mockk<SimHost>()
        every { hostB.state } returns HostState.UP
        every { hostB.model } returns HostModel(4 * 3200.0, 4, 2048)
        every { hostB.availableMemory } returns 512
        every { hostB.isEmpty() } returns true
        scheduler.addHost(hostB.withIds())

        val req = mockk<SchedulingRequest>()
        every { req.task.cpuCoreCount } returns 2
        every { req.task.memorySize } returns 1024
        every { req.task.cpuCapacity } returns 2 * 3200.0
        every { req.isCancelled } returns false

        assertEquals(hostB, scheduler.select(mutableListOf(req).iterator()).host)
    }

    @Test
    fun testInstanceCountFilter() {
        val scheduler =
            FilterScheduler(
                filters = listOf(InstanceCountFilter(limit = 2)),
                weighers = emptyList(),
            )

        val hostA = mockk<SimHost>()
        every { hostA.state } returns HostState.UP
        every { hostA.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.instanceCount } returns 2
        every { hostA.isEmpty() } returns false

        val hostB = mockk<SimHost>()
        every { hostB.state } returns HostState.UP
        every { hostB.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.instanceCount } returns 0
        every { hostB.isEmpty() } returns true

        scheduler.addHost(hostA.withIds())
        scheduler.addHost(hostB.withIds())

        val req = mockk<SchedulingRequest>()
        every { req.task.cpuCoreCount } returns 2
        every { req.task.memorySize } returns 1024
        every { req.isCancelled } returns false

        assertEquals(hostB, scheduler.select(mutableListOf(req).iterator()).host)
    }

    // TODO: fix SameHostFilter
    //    @Test
    fun testAffinityFilter() {
        val scheduler =
            FilterScheduler(
                filters = listOf(SameHostFilter()),
                weighers = emptyList(),
            )

        val reqA = mockk<SchedulingRequest>()
        every { reqA.task.cpuCoreCount } returns 2
        every { reqA.task.memorySize } returns 1024
        every { reqA.isCancelled } returns false
        val taskA = mockk<SimTask>()
        every { taskA.id } returns Random().nextInt(1, Int.MAX_VALUE)
        every { reqA.task } returns taskA

        val hostA = mockk<SimHost>()
        every { hostA.state } returns HostState.UP
        every { hostA.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.getInstances() } returns emptySet()
        every { hostA.provisionedCpuCores } returns 3
        every { hostA.isEmpty() } returns true

        val hostB = mockk<SimHost>()
        every { hostB.state } returns HostState.UP
        every { hostB.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.getInstances() } returns setOf(reqA.task)
        every { hostB.provisionedCpuCores } returns 0
        every { hostB.isEmpty() } returns true

        scheduler.addHost(hostA.withIds())
        scheduler.addHost(hostB.withIds())

        val reqB = mockk<SchedulingRequest>()
        every { reqB.task.cpuCoreCount } returns 2
        every { reqB.task.memorySize } returns 1024
        every { reqB.task.cpuCapacity } returns 0.0
        every { reqB.isCancelled } returns false

        assertEquals(hostA, scheduler.select(mutableListOf(reqB).iterator()).host)

//        every { reqB.task.meta } returns mapOf("scheduler_hint:same_host" to setOf(reqA.task.id))

        assertEquals(hostB, scheduler.select(mutableListOf(reqB).iterator()).host)
    }

    // Fix DifferentHostFilter
//    @Test
    fun testAntiAffinityFilter() {
        val scheduler =
            FilterScheduler(
                filters = listOf(DifferentHostFilter()),
                weighers = emptyList(),
            )

        val reqA = mockk<SchedulingRequest>()
        every { reqA.task.cpuCoreCount } returns 2
        every { reqA.task.memorySize } returns 1024
        every { reqA.isCancelled } returns false
        val taskA = mockk<SimTask>()
        every { taskA.id } returns Random().nextInt(1, Int.MAX_VALUE)
        every { reqA.task } returns taskA

        val hostA = mockk<SimHost>()
        every { hostA.state } returns HostState.UP
        every { hostA.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.getInstances() } returns setOf(reqA.task)
        every { hostA.provisionedCpuCores } returns 3
        every { hostA.isEmpty() } returns true

        val hostB = mockk<SimHost>()
        every { hostB.state } returns HostState.UP
        every { hostB.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.getInstances() } returns emptySet()
        every { hostB.provisionedCpuCores } returns 0
        every { hostB.isEmpty() } returns true

        scheduler.addHost(hostA.withIds())
        scheduler.addHost(hostB.withIds())

        val reqB = mockk<SchedulingRequest>()
        every { reqB.task.cpuCoreCount } returns 2
        every { reqB.task.memorySize } returns 1024
        every { reqB.isCancelled } returns false

        assertEquals(hostA, scheduler.select(mutableListOf(reqB).iterator()).host)

//        every { reqB.task.meta } returns mapOf("scheduler_hint:different_host" to setOf(taskA.id))

        assertEquals(hostB, scheduler.select(mutableListOf(reqB).iterator()).host)
    }

    @Test
    fun testVGPUFilter() {
        val scheduler =
            FilterScheduler(
                filters = listOf(VGpuFilter(1.0)),
                weighers = emptyList(),
            )

        val hostA = mockk<SimHost>()
        every { hostA.state } returns HostState.UP
        every { hostA.model } returns
            HostModel(
                0.0,
                0,
                2048,
                listOf(
                    GpuHostModel(8 * 2600.0, 8, 0L, 0.0),
                ),
            )
        every { hostA.provisionedGpuCores } returns 0
        every { hostA.isEmpty() } returns true
        scheduler.addHost(hostA.withIds())

        val hostB = mockk<SimHost>()
        every { hostB.state } returns HostState.UP
        every { hostB.model } returns
            HostModel(
                0.0,
                0,
                2048,
                listOf(
                    GpuHostModel(8 * 3200.0, 8, 0L, 0.0),
                    GpuHostModel(8 * 3200.0, 8, 0L, 0.0),
                ),
            )
        every { hostB.provisionedGpuCores } returns 0
        every { hostB.isEmpty() } returns true
        scheduler.addHost(hostB.withIds())

        val req = mockk<SchedulingRequest>()
        every { req.task.gpuCoreCount } returns 9
        every { req.task.gpuCapacity } returns 9 * 3200.0
        every { req.isCancelled } returns false

        // filter selects hostB because hostA does not have enough GPU capacity
        assertEquals(hostB, scheduler.select(mutableListOf(req).iterator()).host)
    }

    @Test
    fun testVGPUCapacityFilter() {
        val scheduler =
            FilterScheduler(
                filters = listOf(VGpuCapacityFilter()),
                weighers = emptyList(),
            )

        val hostA = mockk<SimHost>()
        every { hostA.state } returns HostState.UP
        every { hostA.model } returns
            HostModel(
                0.0,
                0,
                2048,
                listOf(
                    GpuHostModel(8 * 2600.0, 8, 0L, 0.0),
                ),
            )
        every { hostA.availableMemory } returns 512
        every { hostA.isEmpty() } returns true
        scheduler.addHost(hostA.withIds())

        val hostB = mockk<SimHost>()
        every { hostB.state } returns HostState.UP
        every { hostB.model } returns
            HostModel(
                0.0,
                0,
                2048,
                listOf(
                    GpuHostModel(8 * 3200.0, 8, 0L, 0.0),
                    GpuHostModel(8 * 3200.0, 8, 0L, 0.0),
                ),
            )
        every { hostB.availableMemory } returns 512
        every { hostB.isEmpty() } returns true
        scheduler.addHost(hostB.withIds())

        val req = mockk<SchedulingRequest>()
        every { req.task.gpuCoreCount } returns 8
        every { req.task.gpuCapacity } returns 8 * 3200.0
        every { req.isCancelled } returns false

        // filter selects hostB because hostA does not have enough GPU capacity
        assertEquals(hostB, scheduler.select(mutableListOf(req).iterator()).host)
    }

    @Test
    fun testRamWeigher() {
        val scheduler =
            FilterScheduler(
                filters = emptyList(),
                weighers = listOf(RamWeigher(1.5)),
            )

        val hostA = mockk<SimHost>()
        every { hostA.state } returns HostState.UP
        every { hostA.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.availableMemory } returns 1024
        every { hostA.availableCpuCores } returns 4
        every { hostA.isEmpty() } returns false

        val hostB = mockk<SimHost>()
        every { hostB.state } returns HostState.UP
        every { hostB.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.availableMemory } returns 512
        every { hostB.availableCpuCores } returns 4
        every { hostB.isEmpty() } returns false

        scheduler.addHost(hostA.withIds())
        scheduler.addHost(hostB.withIds())

        val req = mockk<SchedulingRequest>()
        every { req.task.cpuCoreCount } returns 2
        every { req.task.memorySize } returns 1024
        every { req.isCancelled } returns false

        assertEquals(hostA, scheduler.select(mutableListOf(req).iterator()).host)
    }

    @Test
    fun testCoreRamWeigher() {
        val scheduler =
            FilterScheduler(
                filters = emptyList(),
                weighers = listOf(CoreRamWeigher(1.5)),
            )

        val hostA = mockk<SimHost>()
        every { hostA.state } returns HostState.UP
        every { hostA.model } returns HostModel(12 * 2600.0, 12, 2048)
        every { hostA.availableMemory } returns 1024
        every { hostA.availableCpuCores } returns 12
        every { hostA.isEmpty() } returns true

        val hostB = mockk<SimHost>()
        every { hostB.state } returns HostState.UP
        every { hostB.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.availableMemory } returns 512
        every { hostB.availableCpuCores } returns 4
        every { hostB.isEmpty() } returns true

        scheduler.addHost(hostA.withIds())
        scheduler.addHost(hostB.withIds())

        val req = mockk<SchedulingRequest>()
        every { req.task.cpuCoreCount } returns 2
        every { req.task.memorySize } returns 1024
        every { req.isCancelled } returns false

        assertEquals(hostB, scheduler.select(mutableListOf(req).iterator()).host)
    }

    @Test
    fun testVCpuWeigher() {
        val scheduler =
            FilterScheduler(
                filters = emptyList(),
                weighers = listOf(VCpuWeigher(16.0)),
            )

        val hostA = mockk<SimHost>()
        every { hostA.state } returns HostState.UP
        every { hostA.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.provisionedCpuCores } returns 2
        every { hostA.availableCpuCores } returns 4
        every { hostA.isEmpty() } returns false

        val hostB = mockk<SimHost>()
        every { hostB.state } returns HostState.UP
        every { hostB.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.provisionedCpuCores } returns 0
        every { hostB.availableCpuCores } returns 4
        every { hostB.isEmpty() } returns true

        scheduler.addHost(hostA.withIds())
        scheduler.addHost(hostB.withIds())

        val req = mockk<SchedulingRequest>()
        every { req.task.cpuCoreCount } returns 2
        every { req.task.memorySize } returns 1024
        every { req.isCancelled } returns false

        assertEquals(hostB, scheduler.select(mutableListOf(req).iterator()).host)
    }

    @Test
    fun testEmptyHostsOfDifferentModels() {
        val scheduler = FilterScheduler(filters = listOf(VCpuFilter(1.0)), weighers = emptyList())

        val small = mockHost(cores = 2)
        val large = mockHost(cores = 8)
        scheduler.addHost(small.withIds())
        scheduler.addHost(large.withIds())

        assertEquals(large, scheduler.select(mutableListOf(mockRequest(cores = 4)).iterator()).host)
    }

    @Test
    fun testModelIdOfDifferentModelIsRejected() {
        val scheduler = FilterScheduler(filters = emptyList(), weighers = emptyList())

        val small = mockHost(cores = 2)
        val large = mockHost(cores = 8)
        every { small.name } returns "small"
        every { large.name } returns "large"
        every { small.id } returns 0
        every { large.id } returns 1
        every { small.modelId } returns 0
        every { large.modelId } returns 0

        scheduler.addHost(small)
        assertThrows<IllegalArgumentException> { scheduler.addHost(large) }
    }

    @Test
    fun testEmptyHostsOfOneModelAreTestedOnce() {
        var tests = 0
        val countingFilter = HostFilter { _, _ -> tests++ < 0 }
        val scheduler = FilterScheduler(filters = listOf(countingFilter), weighers = emptyList())

        repeat(100) { scheduler.addHost(mockHost().withIds()) }

        assertEquals(SchedulingResultType.FAILURE, scheduler.select(mutableListOf(mockRequest()).iterator()).resultType)
        assertEquals(1, tests)
    }

    @Test
    fun testNegativeMultiplierPrefersLowestWeight() {
        val scheduler = FilterScheduler(filters = emptyList(), weighers = listOf(RamWeigher(-1.0)))

        val hostA = mockHost(availableMemory = 1024, instances = 1)
        val hostB = mockHost(availableMemory = 512, instances = 1)
        scheduler.addHost(hostA.withIds())
        scheduler.addHost(hostB.withIds())

        assertEquals(hostB, scheduler.select(mutableListOf(mockRequest()).iterator()).host)
    }

    @Test
    fun testCoreRamWeigherNegativeMultiplier() {
        val scheduler = FilterScheduler(filters = emptyList(), weighers = listOf(CoreRamWeigher(-1.0)))

        // 1024 / 12 cores is less memory per core than 512 / 4 cores
        val hostA = mockHost(cores = 12, availableMemory = 1024, instances = 1)
        val hostB = mockHost(cores = 4, availableMemory = 512, instances = 1)
        scheduler.addHost(hostB.withIds())
        scheduler.addHost(hostA.withIds())

        assertEquals(hostA, scheduler.select(mutableListOf(mockRequest()).iterator()).host)
    }

    @Test
    fun testWeightsDoNotCarryOverBetweenSelections() {
        val scheduler = FilterScheduler(filters = emptyList(), weighers = listOf(RamWeigher(1.0)))

        val hostA = mockHost(availableMemory = 512, instances = 1)
        val hostB = mockHost(availableMemory = 1024, instances = 1)
        scheduler.addHost(hostA.withIds())
        scheduler.addHost(hostB.withIds())

        assertEquals(hostB, scheduler.select(mutableListOf(mockRequest()).iterator()).host)
        assertEquals(hostB, scheduler.select(mutableListOf(mockRequest()).iterator()).host)

        every { hostA.availableMemory } returns 1024
        every { hostB.availableMemory } returns 512
        scheduler.updateHost(hostA)
        scheduler.updateHost(hostB)

        assertEquals(hostA, scheduler.select(mutableListOf(mockRequest()).iterator()).host)
    }

    @Test
    fun testWeighersAreCombined() {
        val scheduler =
            FilterScheduler(
                filters = emptyList(),
                weighers = listOf(RamWeigher(1.0), InstanceCountWeigher(-1.0)),
            )

        // A has the most memory, but B has almost as much and far fewer instances
        val hostA = mockHost(memory = 4096, availableMemory = 1024, instances = 3)
        val hostB = mockHost(memory = 4096, availableMemory = 900, instances = 1)
        val hostC = mockHost(memory = 4096, availableMemory = 0, instances = 2)
        scheduler.addHost(hostA.withIds())
        scheduler.addHost(hostB.withIds())
        scheduler.addHost(hostC.withIds())

        assertEquals(hostB, scheduler.select(mutableListOf(mockRequest()).iterator()).host)
    }

    @Test
    fun testMoreHostsThanExpected() {
        val scheduler = FilterScheduler(filters = emptyList(), weighers = listOf(RamWeigher(1.0)), numHosts = 1)

        val hosts = (1..40).map { mockHost(availableMemory = it * 10L, instances = 1) }
        hosts.forEach { scheduler.addHost(it.withIds()) }

        assertEquals(hosts.last(), scheduler.select(mutableListOf(mockRequest()).iterator()).host)
        assertEquals(hosts.last(), scheduler.select(mutableListOf(mockRequest()).iterator()).host)
    }

    @Test
    fun testSelectStopsAtFirstFittingHost() {
        val tested = mutableListOf<SimHost>()
        val recordingFilter = HostFilter { host, _ -> tested.add(host) && host.availableMemory < 3000 }
        val scheduler = FilterScheduler(filters = listOf(recordingFilter), weighers = listOf(RamWeigher(1.0)))

        val hosts = listOf(1000L, 3000L, 2000L).map { mockHost(availableMemory = it, instances = 1) }
        hosts.forEach { scheduler.addHost(it.withIds()) }

        // The host with the most memory is rejected, the second best is taken and the third is never tested
        assertEquals(hosts[2], scheduler.select(mutableListOf(mockRequest()).iterator()).host)
        assertEquals(listOf(hosts[1], hosts[2]), tested)
    }

    @Test
    fun testWideningRangeRanksHostsAgain() {
        val scheduler =
            FilterScheduler(
                filters = emptyList(),
                weighers = listOf(RamWeigher(1.0), InstanceCountWeigher(-1.0)),
            )

        val hostA = mockHost(memory = 4096, availableMemory = 1000, instances = 2)
        val hostB = mockHost(memory = 4096, availableMemory = 900, instances = 1)
        val hostC = mockHost(memory = 4096, availableMemory = 800, instances = 1)
        listOf(hostA, hostB, hostC).forEach { scheduler.addHost(it.withIds()) }
        every { hostB.instanceCount } returns 0
        scheduler.updateHost(hostB)

        // Memory ranges over 200 and instances over 2: B scores 0.5, A scores 1 - 1 = 0
        assertEquals(hostB, scheduler.select(mutableListOf(mockRequest()).iterator()).host)

        // A host with many instances widens the instance range to 10, so instances weigh less: A scores 1 - 0.2 = 0.8
        scheduler.addHost(mockHost(memory = 4096, availableMemory = 850, instances = 10).withIds())

        assertEquals(hostA, scheduler.select(mutableListOf(mockRequest()).iterator()).host)
    }

    @Test
    fun testEmptyHostsTakeTurnsStandingInForTheirGroup() {
        val scheduler = FilterScheduler(filters = emptyList(), weighers = listOf(RamWeigher(1.0)))

        val first = mockHost()
        val second = mockHost()
        scheduler.addHost(first.withIds())
        scheduler.addHost(second.withIds())
        assertEquals(first, scheduler.select(mutableListOf(mockRequest()).iterator()).host)

        // Once the first host runs a task, the second stands in for the empty hosts and has more memory
        every { first.isEmpty() } returns false
        every { first.availableMemory } returns 1024
        scheduler.updateHost(first)
        assertEquals(second, scheduler.select(mutableListOf(mockRequest()).iterator()).host)

        // When the first host is empty again, it takes over because its id is lower
        every { first.isEmpty() } returns true
        every { first.availableMemory } returns 2048
        scheduler.updateHost(first)
        assertEquals(first, scheduler.select(mutableListOf(mockRequest()).iterator()).host)
    }

    @Test
    fun testFailedHostIsNotSelected() {
        val scheduler = FilterScheduler(filters = emptyList(), weighers = listOf(RamWeigher(1.0)))

        val best = mockHost(availableMemory = 2000, instances = 1)
        val other = mockHost(availableMemory = 1000, instances = 1)
        scheduler.addHost(best.withIds())
        scheduler.addHost(other.withIds())

        every { best.state } returns HostState.ERROR
        scheduler.failHost(best)
        assertEquals(other, scheduler.select(mutableListOf(mockRequest()).iterator()).host)

        every { best.state } returns HostState.UP
        scheduler.restartHost(best)
        assertEquals(best, scheduler.select(mutableListOf(mockRequest()).iterator()).host)
    }

    @Test
    fun testDelayedTaskStaysQueuedWhileNextTaskIsPlaced() {
        val scheduler = FilterScheduler(filters = emptyList(), weighers = emptyList(), timeshifter = { it.deferrable })
        val host = mockHost()
        scheduler.addHost(host.withIds())

        val delayed = mockRequest()
        every { delayed.task.deferrable } returns true
        val next = mockRequest()
        every { next.task.deferrable } returns false
        val queue = mutableListOf(delayed, next)

        val result = scheduler.select(queue.iterator())

        assertEquals(next, result.req)
        assertEquals(host, result.host)
        assertEquals(listOf(delayed), queue)
    }

    @Test
    fun testOnlyDelayedTasksLeaveNothingToSchedule() {
        val scheduler = FilterScheduler(filters = emptyList(), weighers = emptyList(), timeshifter = { true })
        scheduler.addHost(mockHost().withIds())

        val queue = mutableListOf(mockRequest(), mockRequest())

        assertEquals(SchedulingResultType.EMPTY, scheduler.select(queue.iterator()).resultType)
        assertEquals(2, queue.size)
    }

    @Test
    fun testFirstTaskThatMayStartDecidesFailure() {
        val scheduler = FilterScheduler(filters = emptyList(), weighers = emptyList(), timeshifter = { it.deferrable })

        val delayed = mockRequest()
        every { delayed.task.deferrable } returns true
        val next = mockRequest()
        every { next.task.deferrable } returns false

        val result = scheduler.select(mutableListOf(delayed, next).iterator())

        assertEquals(SchedulingResultType.FAILURE, result.resultType)
        assertEquals(next, result.req)
    }

    @Test
    fun testMatchesReferenceWithTwoWeighers() {
        checkAgainstReference(listOf(RamWeigher(1.0), InstanceCountWeigher(-0.5)), seed = 1)
    }

    @Test
    fun testMatchesReferenceWithNegativeWeigher() {
        checkAgainstReference(listOf(CoreRamWeigher(-1.0)), seed = 2)
    }

    @Test
    fun testMatchesReferenceWithoutWeighers() {
        checkAgainstReference(emptyList(), seed = 3)
    }

    /** Real hosts with a mocked engine; tasks are placed and removed the way [SimHost.spawn] and [SimHost.delete] do. */
    private val tasksField = SimHost::class.java.getDeclaredField("tasks").apply { isAccessible = true }

    @Suppress("UNCHECKED_CAST")
    private fun SimHost.tasks(): MutableSet<SimTask> = tasksField.get(this) as MutableSet<SimTask>

    private fun realHost(
        id: Int,
        modelId: Int,
        cores: Int,
        memory: Long,
    ): SimHost {
        val machine =
            MachineModel(
                CpuModel(0, cores, 2600.0, "vendor", "model", "arch"),
                MemoryUnit("vendor", "model", 3200.0, memory),
                null,
                FlowDistributorFactory.DistributionPolicy.MAX_MIN_FAIRNESS,
                FlowDistributorFactory.DistributionPolicy.MAX_MIN_FAIRNESS,
            )
        val host =
            SimHost(
                id,
                "H$id",
                "C01",
                InstantSource.fixed(Instant.EPOCH),
                mockk<FlowEngine>(relaxed = true),
                machine,
                mockk<PowerModel>(relaxed = true),
                null,
                0.0,
                1.0,
                mockk<FlowDistributor>(relaxed = true),
            )
        host.modelId = modelId
        return host
    }

    /**
     * Places and finishes random tasks and fails and restarts random hosts, checking every selection against a naive
     * reference: test every available host, score each with the weight ranges seen so far, and take the highest score,
     * the lowest id on a tie.
     */
    private fun checkAgainstReference(
        weighers: List<HostWeigher>,
        seed: Long,
    ) {
        val filters = listOf(ComputeFilter(), VCpuFilter(1.0), RamFilter(1.0), InstanceCountFilter(6))
        // Small blocks, so the ranking has many of them and they are split, merged and skipped
        val scheduler = FilterScheduler(filters, weighers, numHosts = 200, blockSize = 8)
        val random = Random(seed)

        val models = listOf(8 to 16_384L, 16 to 32_768L, 32 to 65_536L)
        val hosts =
            List(200) {
                val (cores, memory) = models[it % 3]
                realHost(it, it % 3, cores, memory)
            }

        val min = DoubleArray(weighers.size) { Double.POSITIVE_INFINITY }
        val max = DoubleArray(weighers.size) { Double.NEGATIVE_INFINITY }

        fun observe(host: SimHost) {
            for ((i, weigher) in weighers.withIndex()) {
                min[i] = minOf(min[i], weigher.getWeight(host))
                max[i] = maxOf(max[i], weigher.getWeight(host))
            }
        }

        fun score(host: SimHost): Double {
            var score = 0.0
            for ((i, weigher) in weighers.withIndex()) {
                val range = max[i] - min[i]
                val factor = if (range > 0.0) weigher.multiplier / range else 0.0
                score += factor * (weigher.getWeight(host) - min[i])
            }
            return score
        }

        hosts.forEach {
            scheduler.addHost(it)
            observe(it)
        }

        repeat(3000) { step ->
            val roll = random.nextInt(100)
            val host = hosts[random.nextInt(hosts.size)]
            when {
                roll < 60 -> {
                    val cores = 1 + random.nextInt(8)
                    // Up to 24 GiB, so memory rather than cores sometimes decides
                    val memory = 512 * (1 + random.nextInt(48))
                    val task = SimTask(step, 0L, 1000L, cores, cores * 2600.0, memory, 0, 0.0, 0, null, false, Long.MAX_VALUE, null, null)

                    val expected =
                        hosts
                            .filter { candidate -> filters.all { it.test(candidate, task) } }
                            .sortedWith(compareByDescending<SimHost> { score(it) }.thenBy { it.id })
                            .firstOrNull()
                    val actual = scheduler.select(mutableListOf(SchedulingRequest(task, 0L)).iterator()).host
                    assertEquals(expected, actual) { "Different host chosen at step $step" }

                    if (actual != null) {
                        actual.tasks().add(task)
                        actual.reserve(task)
                        scheduler.updateHost(actual)
                        observe(actual)
                    }
                }
                roll < 90 && host.state == HostState.UP && !host.isEmpty() -> {
                    val task = host.tasks().first()
                    host.tasks().remove(task)
                    host.release(task)
                    scheduler.updateHost(host)
                    observe(host)
                }
                roll < 95 && host.state == HostState.UP -> {
                    // As a failing host does: it leaves the pool, then its tasks are removed
                    val tasks = host.tasks().toList()
                    host.tasks().clear()
                    host.fail()
                    scheduler.failHost(host)
                    tasks.forEach { host.release(it) }
                    scheduler.updateHost(host)
                }
                roll >= 95 && host.state == HostState.ERROR -> {
                    host.recover()
                    scheduler.restartHost(host)
                    observe(host)
                }
            }
        }
    }

    /** Model ids handed out by [withIds]. */
    private val modelIds = HashMap<HostModel, Int>()
    private var nextHostId = 0

    /** Stub [SimHost.id], and [SimHost.modelId] from the stubbed model, the way the SDK's HostsProvisioningStep assigns them. */
    private fun SimHost.withIds(): SimHost {
        every { id } returns nextHostId++
        every { modelId } returns modelIds.getOrPut(model) { modelIds.size }
        return this
    }

    /** A host that is empty when it runs no [instances]. */
    private fun mockHost(
        cores: Int = 4,
        memory: Long = 2048,
        availableCores: Int = cores,
        availableMemory: Long = memory,
        instances: Int = 0,
    ): SimHost {
        val host = mockk<SimHost>()
        every { host.state } returns HostState.UP
        every { host.model } returns HostModel(cores * 2600.0, cores, memory)
        every { host.availableCpuCores } returns availableCores
        every { host.provisionedCpuCores } returns cores - availableCores
        every { host.availableMemory } returns availableMemory
        every { host.instanceCount } returns instances
        every { host.isEmpty() } returns (instances == 0)
        return host
    }

    private fun mockRequest(
        cores: Int = 1,
        memory: Int = 1024,
    ): SchedulingRequest {
        val req = mockk<SchedulingRequest>()
        every { req.task.cpuCoreCount } returns cores
        every { req.task.memorySize } returns memory
        every { req.isCancelled } returns false
        return req
    }
}
