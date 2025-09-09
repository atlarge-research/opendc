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

package org.opendc.compute.simulator.scheduler

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.opendc.compute.simulator.host.GpuHostModel
import org.opendc.compute.simulator.host.HostModel
import org.opendc.compute.simulator.host.HostState
import org.opendc.compute.simulator.scheduler.filters.ComputeFilter
import org.opendc.compute.simulator.scheduler.filters.DifferentHostFilter
import org.opendc.compute.simulator.scheduler.filters.InstanceCountFilter
import org.opendc.compute.simulator.scheduler.filters.RamFilter
import org.opendc.compute.simulator.scheduler.filters.SameHostFilter
import org.opendc.compute.simulator.scheduler.filters.VCpuCapacityFilter
import org.opendc.compute.simulator.scheduler.filters.VCpuFilter
import org.opendc.compute.simulator.scheduler.filters.VGpuCapacityFilter
import org.opendc.compute.simulator.scheduler.filters.VGpuFilter
import org.opendc.compute.simulator.scheduler.weights.CoreRamWeigher
import org.opendc.compute.simulator.scheduler.weights.InstanceCountWeigher
import org.opendc.compute.simulator.scheduler.weights.RamWeigher
import org.opendc.compute.simulator.scheduler.weights.VCpuWeigher
import org.opendc.compute.simulator.service.HostView
import org.opendc.compute.simulator.service.ServiceTask
import java.util.Random

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
        every { req.task.flavor.cpuCoreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
        every { req.isCancelled } returns false

        assertEquals(SchedulingResultType.FAILURE, scheduler.select(mutableListOf(req).iterator()).resultType)
    }

    @Test
    fun testNoFiltersAndSchedulers() {
        val scheduler =
            FilterScheduler(
                filters = emptyList(),
                weighers = emptyList(),
            )

        val hostA = mockk<HostView>()
        every { hostA.host.getState() } returns HostState.DOWN

        val hostB = mockk<HostView>()
        every { hostB.host.getState() } returns HostState.UP

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.cpuCoreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
        every { req.isCancelled } returns false

        // Make sure we get the first host both times
        assertAll(
            { assertEquals(hostA, scheduler.select(mutableListOf(req).iterator()).host) },
            { assertEquals(hostA, scheduler.select(mutableListOf(req).iterator()).host) },
        )
    }

    @Test
    fun testNoFiltersAndSchedulersRandom() {
        val scheduler =
            FilterScheduler(
                filters = emptyList(),
                weighers = emptyList(),
                subsetSize = Int.MAX_VALUE,
                random = Random(1),
            )

        val hostA = mockk<HostView>()
        every { hostA.host.getState() } returns HostState.DOWN

        val hostB = mockk<HostView>()
        every { hostB.host.getState() } returns HostState.UP

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.cpuCoreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
        every { req.isCancelled } returns false

        // Make sure we get the first host both times
        assertAll(
            { assertEquals(hostB, scheduler.select(mutableListOf(req).iterator()).host) },
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

        val host = mockk<HostView>()
        every { host.host.getState() } returns HostState.DOWN

        scheduler.addHost(host)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.cpuCoreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
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

        val host = mockk<HostView>()
        every { host.host.getState() } returns HostState.UP

        scheduler.addHost(host)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.cpuCoreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
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

        val hostA = mockk<HostView>()
        every { hostA.host.getState() } returns HostState.UP
        every { hostA.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.availableMemory } returns 512

        val hostB = mockk<HostView>()
        every { hostB.host.getState() } returns HostState.UP
        every { hostB.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.availableMemory } returns 2048

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.cpuCoreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
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

        val host = mockk<HostView>()
        every { host.host.getState() } returns HostState.UP
        every { host.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { host.availableMemory } returns 2048

        scheduler.addHost(host)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.cpuCoreCount } returns 2
        every { req.task.flavor.memorySize } returns 2300
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

        val hostA = mockk<HostView>()
        every { hostA.host.getState() } returns HostState.UP
        every { hostA.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.provisionedCpuCores } returns 3

        val hostB = mockk<HostView>()
        every { hostB.host.getState() } returns HostState.UP
        every { hostB.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.provisionedCpuCores } returns 0

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.cpuCoreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
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

        val host = mockk<HostView>()
        every { host.host.getState() } returns HostState.UP
        every { host.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { host.provisionedCpuCores } returns 0

        scheduler.addHost(host)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.cpuCoreCount } returns 8
        every { req.task.flavor.memorySize } returns 1024
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

        val hostA = mockk<HostView>()
        every { hostA.host.getState() } returns HostState.UP
        every { hostA.host.getModel() } returns HostModel(8 * 2600.0, 8, 2048)
        every { hostA.availableMemory } returns 512
        scheduler.addHost(hostA)

        val hostB = mockk<HostView>()
        every { hostB.host.getState() } returns HostState.UP
        every { hostB.host.getModel() } returns HostModel(4 * 3200.0, 4, 2048)
        every { hostB.availableMemory } returns 512
        scheduler.addHost(hostB)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.cpuCoreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
        every { req.task.flavor.meta } returns mapOf("cpu-capacity" to 2 * 3200.0)
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

        val hostA = mockk<HostView>()
        every { hostA.host.getState() } returns HostState.UP
        every { hostA.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.instanceCount } returns 2

        val hostB = mockk<HostView>()
        every { hostB.host.getState() } returns HostState.UP
        every { hostB.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.instanceCount } returns 0

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.cpuCoreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
        every { req.isCancelled } returns false

        assertEquals(hostB, scheduler.select(mutableListOf(req).iterator()).host)
    }

    @Test
    fun testAffinityFilter() {
        val scheduler =
            FilterScheduler(
                filters = listOf(SameHostFilter()),
                weighers = emptyList(),
            )

        val reqA = mockk<SchedulingRequest>()
        every { reqA.task.flavor.cpuCoreCount } returns 2
        every { reqA.task.flavor.memorySize } returns 1024
        every { reqA.isCancelled } returns false
        val taskA = mockk<ServiceTask>()
        every { taskA.id } returns Random().nextInt(1, Int.MAX_VALUE)
        every { reqA.task } returns taskA

        val hostA = mockk<HostView>()
        every { hostA.host.getState() } returns HostState.UP
        every { hostA.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.host.getInstances() } returns emptySet()
        every { hostA.provisionedCpuCores } returns 3

        val hostB = mockk<HostView>()
        every { hostB.host.getState() } returns HostState.UP
        every { hostB.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.host.getInstances() } returns setOf(reqA.task)
        every { hostB.provisionedCpuCores } returns 0

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val reqB = mockk<SchedulingRequest>()
        every { reqB.task.flavor.cpuCoreCount } returns 2
        every { reqB.task.flavor.memorySize } returns 1024
        every { reqB.task.meta } returns emptyMap()
        every { reqB.isCancelled } returns false

        assertEquals(hostA, scheduler.select(mutableListOf(reqB).iterator()).host)

        every { reqB.task.meta } returns mapOf("scheduler_hint:same_host" to setOf(reqA.task.id))

        assertEquals(hostB, scheduler.select(mutableListOf(reqB).iterator()).host)
    }

    @Test
    fun testAntiAffinityFilter() {
        val scheduler =
            FilterScheduler(
                filters = listOf(DifferentHostFilter()),
                weighers = emptyList(),
            )

        val reqA = mockk<SchedulingRequest>()
        every { reqA.task.flavor.cpuCoreCount } returns 2
        every { reqA.task.flavor.memorySize } returns 1024
        every { reqA.isCancelled } returns false
        val taskA = mockk<ServiceTask>()
        every { taskA.id } returns Random().nextInt(1, Int.MAX_VALUE)
        every { reqA.task } returns taskA

        val hostA = mockk<HostView>()
        every { hostA.host.getState() } returns HostState.UP
        every { hostA.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.host.getInstances() } returns setOf(reqA.task)
        every { hostA.provisionedCpuCores } returns 3

        val hostB = mockk<HostView>()
        every { hostB.host.getState() } returns HostState.UP
        every { hostB.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.host.getInstances() } returns emptySet()
        every { hostB.provisionedCpuCores } returns 0

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val reqB = mockk<SchedulingRequest>()
        every { reqB.task.flavor.cpuCoreCount } returns 2
        every { reqB.task.flavor.memorySize } returns 1024
        every { reqB.task.meta } returns emptyMap()
        every { reqB.isCancelled } returns false

        assertEquals(hostA, scheduler.select(mutableListOf(reqB).iterator()).host)

        every { reqB.task.meta } returns mapOf("scheduler_hint:different_host" to setOf(taskA.id))

        assertEquals(hostB, scheduler.select(mutableListOf(reqB).iterator()).host)
    }

    @Test
    fun testVGPUFilter() {
        val scheduler =
            FilterScheduler(
                filters = listOf(VGpuFilter(1.0)),
                weighers = emptyList(),
            )

        val hostA = mockk<HostView>()
        every { hostA.host.getState() } returns HostState.UP
        every { hostA.host.getModel() } returns HostModel(0.0, 0, 2048,
            listOf(
                GpuHostModel(8 * 2600.0, 8, 0L, 0.0)
            ))
        every { hostA.provisionedGpuCores } returns 0
        scheduler.addHost(hostA)

        val hostB = mockk<HostView>()
        every { hostB.host.getState() } returns HostState.UP
        every { hostB.host.getModel() } returns HostModel(0.0, 0, 2048,
            listOf(
                GpuHostModel(8 * 3200.0, 8, 0L, 0.0),
                GpuHostModel(8 * 3200.0, 8, 0L, 0.0)
            ))
        every { hostB.provisionedGpuCores } returns 0
        scheduler.addHost(hostB)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.gpuCoreCount } returns 9
        every { req.task.flavor.meta } returns mapOf("gpu-capacity" to 9 * 3200.0)
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

        val hostA = mockk<HostView>()
        every { hostA.host.getState() } returns HostState.UP
        every { hostA.host.getModel() } returns HostModel(0.0, 0, 2048,
            listOf(
                GpuHostModel(8 * 2600.0, 8, 0L, 0.0)
            ))
        every { hostA.availableMemory } returns 512
        scheduler.addHost(hostA)

        val hostB = mockk<HostView>()
        every { hostB.host.getState() } returns HostState.UP
        every { hostB.host.getModel() } returns HostModel(0.0, 0, 2048,
            listOf(
                GpuHostModel(8 * 3200.0, 8, 0L, 0.0),
                GpuHostModel(8 * 3200.0, 8, 0L, 0.0)
            ))
        every { hostB.availableMemory } returns 512
        scheduler.addHost(hostB)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.gpuCoreCount } returns 8
        every { req.task.flavor.meta } returns mapOf("gpu-capacity" to 8 * 3200.0)
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

        val hostA = mockk<HostView>()
        every { hostA.host.getState() } returns HostState.UP
        every { hostA.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.availableMemory } returns 1024

        val hostB = mockk<HostView>()
        every { hostB.host.getState() } returns HostState.UP
        every { hostB.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.availableMemory } returns 512

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.cpuCoreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
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

        val hostA = mockk<HostView>()
        every { hostA.host.getState() } returns HostState.UP
        every { hostA.host.getModel() } returns HostModel(12 * 2600.0, 12, 2048)
        every { hostA.availableMemory } returns 1024

        val hostB = mockk<HostView>()
        every { hostB.host.getState() } returns HostState.UP
        every { hostB.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.availableMemory } returns 512

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.cpuCoreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
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

        val hostA = mockk<HostView>()
        every { hostA.host.getState() } returns HostState.UP
        every { hostA.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.provisionedCpuCores } returns 2

        val hostB = mockk<HostView>()
        every { hostB.host.getState() } returns HostState.UP
        every { hostB.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.provisionedCpuCores } returns 0

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.cpuCoreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
        every { req.isCancelled } returns false

        assertEquals(hostB, scheduler.select(mutableListOf(req).iterator()).host)
    }

    @Test
    fun testInstanceCountWeigher() {
        val scheduler =
            FilterScheduler(
                filters = emptyList(),
                weighers = listOf(InstanceCountWeigher(multiplier = -1.0)),
            )

        val hostA = mockk<HostView>()
        every { hostA.host.getState() } returns HostState.UP
        every { hostA.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.instanceCount } returns 2

        val hostB = mockk<HostView>()
        every { hostB.host.getState() } returns HostState.UP
        every { hostB.host.getModel() } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.instanceCount } returns 0

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val req = mockk<SchedulingRequest>()
        every { req.task.flavor.cpuCoreCount } returns 2
        every { req.task.flavor.memorySize } returns 1024
        every { req.isCancelled } returns false

        assertEquals(hostB, scheduler.select(mutableListOf(req).iterator()).host)
    }
}
