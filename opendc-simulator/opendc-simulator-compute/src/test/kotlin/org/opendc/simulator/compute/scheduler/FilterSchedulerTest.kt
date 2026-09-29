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
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.compute.infrastructure.SimHost
import org.opendc.simulator.compute.models.GpuHostModel
import org.opendc.simulator.compute.models.HostModel
import org.opendc.simulator.compute.models.HostState
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
import org.opendc.simulator.compute.scheduler.weights.InstanceCountWeigher
import org.opendc.simulator.compute.scheduler.weights.RamWeigher
import org.opendc.simulator.compute.scheduler.weights.VCpuWeigher
import org.opendc.simulator.compute.task.SimTask
import java.util.Random
import java.util.SplittableRandom

/**
 * Test suite for the [FilterScheduler].
 */
internal class FilterSchedulerTest {
    @Test
    fun testInvalidSubsetSize() {
        assertThrows<IllegalArgumentException> {
            FilterScheduler(
                filters = emptyList(),
                weighers = emptyList(),
                subsetSize = 0,
            )
        }

        assertThrows<IllegalArgumentException> {
            FilterScheduler(
                filters = emptyList(),
                weighers = emptyList(),
                subsetSize = -2,
            )
        }
    }

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
    fun testSortableFilterNarrowsUsedHosts() {
        val tested = mutableListOf<SimHost>()
        val recordingFilter = HostFilter { host, _ -> tested.add(host) }
        val scheduler = FilterScheduler(filters = listOf(recordingFilter, VCpuFilter(1.0)), weighers = emptyList())

        val hosts = (0..7).map { mockHost(cores = 8, availableCores = it, instances = 1) }
        hosts.shuffled(Random(0)).forEach { scheduler.addHost(it.withIds()) }

        // The hosts are sorted on their available cores, so only the two with 6 or more are tested by the other filter
        assertEquals(hosts[6], scheduler.select(mutableListOf(mockRequest(cores = 6)).iterator()).host)
        assertEquals(setOf(hosts[6], hosts[7]), tested.toSet())
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
    fun testMoreCandidatesThanExpectedHosts() {
        val scheduler = FilterScheduler(filters = emptyList(), weighers = listOf(RamWeigher(1.0)), numHosts = 1)

        val hosts = (1..40).map { mockHost(availableMemory = it * 10L, instances = 1) }
        hosts.forEach { scheduler.addHost(it.withIds()) }

        assertEquals(hosts.last(), scheduler.select(mutableListOf(mockRequest()).iterator()).host)
        assertEquals(hosts.last(), scheduler.select(mutableListOf(mockRequest()).iterator()).host)
    }

    @Test
    fun testSubsetChoosesAmongBestHosts() {
        val scheduler =
            FilterScheduler(
                filters = emptyList(),
                weighers = listOf(RamWeigher(1.0)),
                subsetSize = 2,
                random = SplittableRandom(0),
            )

        val hostA = mockHost(availableMemory = 3000, instances = 1)
        val hostB = mockHost(availableMemory = 2000, instances = 1)
        val hostC = mockHost(availableMemory = 1000, instances = 1)
        scheduler.addHost(hostC.withIds())
        scheduler.addHost(hostB.withIds())
        scheduler.addHost(hostA.withIds())

        val chosen = List(100) { scheduler.select(mutableListOf(mockRequest()).iterator()).host }

        assertEquals(setOf(hostA, hostB), chosen.toSet())
    }

    @Test
    fun testRandomChoiceCountsEveryEmptyHost() {
        // One used host and three identical empty hosts: a uniform choice picks the used host a quarter of the time
        val usedHostChosen =
            (0L until 400L).count { seed ->
                val scheduler =
                    FilterScheduler(
                        filters = emptyList(),
                        weighers = emptyList(),
                        subsetSize = Int.MAX_VALUE,
                        random = SplittableRandom(seed),
                    )
                val usedHost = mockHost(availableCores = 2, instances = 1)
                scheduler.addHost(usedHost.withIds())
                repeat(3) { scheduler.addHost(mockHost().withIds()) }

                scheduler.select(mutableListOf(mockRequest()).iterator()).host === usedHost
            }

        assertTrue(usedHostChosen in 60..140) { "Used host chosen $usedHostChosen out of 400 times" }
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
