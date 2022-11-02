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

package org.opendc.compute.service.scheduler

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.opendc.compute.api.Server
import org.opendc.compute.service.HostView
import org.opendc.compute.service.driver.HostModel
import org.opendc.compute.service.driver.HostState
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.filters.DifferentHostFilter
import org.opendc.compute.service.scheduler.filters.InstanceCountFilter
import org.opendc.compute.service.scheduler.filters.RamFilter
import org.opendc.compute.service.scheduler.filters.SameHostFilter
import org.opendc.compute.service.scheduler.filters.VCpuCapacityFilter
import org.opendc.compute.service.scheduler.filters.VCpuFilter
import org.opendc.compute.service.scheduler.weights.CoreRamWeigher
import org.opendc.compute.service.scheduler.weights.InstanceCountWeigher
import org.opendc.compute.service.scheduler.weights.RamWeigher
import org.opendc.compute.service.scheduler.weights.VCpuWeigher
import java.util.Random
import java.util.UUID

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
                subsetSize = 0
            )
        }

        assertThrows<IllegalArgumentException> {
            FilterScheduler(
                filters = emptyList(),
                weighers = emptyList(),
                subsetSize = -2
            )
        }
    }

    @Test
    fun testNoHosts() {
        val scheduler = FilterScheduler(
            filters = emptyList(),
            weighers = emptyList()
        )

        val server = mockk<Server>()
        every { server.flavor.cpuCount } returns 2
        every { server.flavor.memorySize } returns 1024

        assertNull(scheduler.select(server))
    }

    @Test
    fun testNoFiltersAndSchedulers() {
        val scheduler = FilterScheduler(
            filters = emptyList(),
            weighers = emptyList()
        )

        val hostA = mockk<HostView>()
        every { hostA.host.state } returns HostState.DOWN

        val hostB = mockk<HostView>()
        every { hostB.host.state } returns HostState.UP

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val server = mockk<Server>()
        every { server.flavor.cpuCount } returns 2
        every { server.flavor.memorySize } returns 1024

        // Make sure we get the first host both times
        assertAll(
            { assertEquals(hostA, scheduler.select(server)) },
            { assertEquals(hostA, scheduler.select(server)) }
        )
    }

    @Test
    fun testNoFiltersAndSchedulersRandom() {
        val scheduler = FilterScheduler(
            filters = emptyList(),
            weighers = emptyList(),
            subsetSize = Int.MAX_VALUE,
            random = Random(1)
        )

        val hostA = mockk<HostView>()
        every { hostA.host.state } returns HostState.DOWN

        val hostB = mockk<HostView>()
        every { hostB.host.state } returns HostState.UP

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val server = mockk<Server>()
        every { server.flavor.cpuCount } returns 2
        every { server.flavor.memorySize } returns 1024

        // Make sure we get the first host both times
        assertAll(
            { assertEquals(hostB, scheduler.select(server)) },
            { assertEquals(hostA, scheduler.select(server)) }
        )
    }

    @Test
    fun testHostIsDown() {
        val scheduler = FilterScheduler(
            filters = listOf(ComputeFilter()),
            weighers = emptyList()
        )

        val host = mockk<HostView>()
        every { host.host.state } returns HostState.DOWN

        scheduler.addHost(host)

        val server = mockk<Server>()
        every { server.flavor.cpuCount } returns 2
        every { server.flavor.memorySize } returns 1024

        assertNull(scheduler.select(server))
    }

    @Test
    fun testHostIsUp() {
        val scheduler = FilterScheduler(
            filters = listOf(ComputeFilter()),
            weighers = emptyList()
        )

        val host = mockk<HostView>()
        every { host.host.state } returns HostState.UP

        scheduler.addHost(host)

        val server = mockk<Server>()
        every { server.flavor.cpuCount } returns 2
        every { server.flavor.memorySize } returns 1024

        assertEquals(host, scheduler.select(server))
    }

    @Test
    fun testRamFilter() {
        val scheduler = FilterScheduler(
            filters = listOf(RamFilter(1.0)),
            weighers = emptyList()
        )

        val hostA = mockk<HostView>()
        every { hostA.host.state } returns HostState.UP
        every { hostA.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.availableMemory } returns 512

        val hostB = mockk<HostView>()
        every { hostB.host.state } returns HostState.UP
        every { hostB.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.availableMemory } returns 2048

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val server = mockk<Server>()
        every { server.flavor.cpuCount } returns 2
        every { server.flavor.memorySize } returns 1024

        assertEquals(hostB, scheduler.select(server))
    }

    @Test
    fun testRamFilterOvercommit() {
        val scheduler = FilterScheduler(
            filters = listOf(RamFilter(1.5)),
            weighers = emptyList()
        )

        val host = mockk<HostView>()
        every { host.host.state } returns HostState.UP
        every { host.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { host.availableMemory } returns 2048

        scheduler.addHost(host)

        val server = mockk<Server>()
        every { server.flavor.cpuCount } returns 2
        every { server.flavor.memorySize } returns 2300

        assertNull(scheduler.select(server))
    }

    @Test
    fun testVCpuFilter() {
        val scheduler = FilterScheduler(
            filters = listOf(VCpuFilter(1.0)),
            weighers = emptyList()
        )

        val hostA = mockk<HostView>()
        every { hostA.host.state } returns HostState.UP
        every { hostA.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.provisionedCores } returns 3

        val hostB = mockk<HostView>()
        every { hostB.host.state } returns HostState.UP
        every { hostB.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.provisionedCores } returns 0

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val server = mockk<Server>()
        every { server.flavor.cpuCount } returns 2
        every { server.flavor.memorySize } returns 1024

        assertEquals(hostB, scheduler.select(server))
    }

    @Test
    fun testVCpuFilterOvercommit() {
        val scheduler = FilterScheduler(
            filters = listOf(VCpuFilter(16.0)),
            weighers = emptyList()
        )

        val host = mockk<HostView>()
        every { host.host.state } returns HostState.UP
        every { host.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { host.provisionedCores } returns 0

        scheduler.addHost(host)

        val server = mockk<Server>()
        every { server.flavor.cpuCount } returns 8
        every { server.flavor.memorySize } returns 1024

        assertNull(scheduler.select(server))
    }

    @Test
    fun testVCpuCapacityFilter() {
        val scheduler = FilterScheduler(
            filters = listOf(VCpuCapacityFilter()),
            weighers = emptyList()
        )

        val hostA = mockk<HostView>()
        every { hostA.host.state } returns HostState.UP
        every { hostA.host.model } returns HostModel(8 * 2600.0, 8, 2048)
        every { hostA.availableMemory } returns 512
        scheduler.addHost(hostA)

        val hostB = mockk<HostView>()
        every { hostB.host.state } returns HostState.UP
        every { hostB.host.model } returns HostModel(4 * 3200.0, 4, 2048)
        every { hostB.availableMemory } returns 512

        scheduler.addHost(hostB)

        val server = mockk<Server>()
        every { server.flavor.cpuCount } returns 2
        every { server.flavor.memorySize } returns 1024
        every { server.flavor.meta } returns mapOf("cpu-capacity" to 2 * 3200.0)

        assertEquals(hostB, scheduler.select(server))
    }

    @Test
    fun testInstanceCountFilter() {
        val scheduler = FilterScheduler(
            filters = listOf(InstanceCountFilter(limit = 2)),
            weighers = emptyList()
        )

        val hostA = mockk<HostView>()
        every { hostA.host.state } returns HostState.UP
        every { hostA.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.instanceCount } returns 2

        val hostB = mockk<HostView>()
        every { hostB.host.state } returns HostState.UP
        every { hostB.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.instanceCount } returns 0

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val server = mockk<Server>()
        every { server.flavor.cpuCount } returns 2
        every { server.flavor.memorySize } returns 1024

        assertEquals(hostB, scheduler.select(server))
    }

    @Test
    fun testAffinityFilter() {
        val scheduler = FilterScheduler(
            filters = listOf(SameHostFilter()),
            weighers = emptyList()
        )

        val serverA = mockk<Server>()
        every { serverA.uid } returns UUID.randomUUID()
        every { serverA.flavor.cpuCount } returns 2
        every { serverA.flavor.memorySize } returns 1024

        val hostA = mockk<HostView>()
        every { hostA.host.state } returns HostState.UP
        every { hostA.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.host.instances } returns emptySet()
        every { hostA.provisionedCores } returns 3

        val hostB = mockk<HostView>()
        every { hostB.host.state } returns HostState.UP
        every { hostB.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.host.instances } returns setOf(serverA)
        every { hostB.provisionedCores } returns 0

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val serverB = mockk<Server>()
        every { serverB.flavor.cpuCount } returns 2
        every { serverB.flavor.memorySize } returns 1024
        every { serverB.meta } returns emptyMap()

        assertEquals(hostA, scheduler.select(serverB))

        every { serverB.meta } returns mapOf("scheduler_hint:same_host" to setOf(serverA.uid))

        assertEquals(hostB, scheduler.select(serverB))
    }

    @Test
    fun testAntiAffinityFilter() {
        val scheduler = FilterScheduler(
            filters = listOf(DifferentHostFilter()),
            weighers = emptyList()
        )

        val serverA = mockk<Server>()
        every { serverA.uid } returns UUID.randomUUID()
        every { serverA.flavor.cpuCount } returns 2
        every { serverA.flavor.memorySize } returns 1024

        val hostA = mockk<HostView>()
        every { hostA.host.state } returns HostState.UP
        every { hostA.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.host.instances } returns setOf(serverA)
        every { hostA.provisionedCores } returns 3

        val hostB = mockk<HostView>()
        every { hostB.host.state } returns HostState.UP
        every { hostB.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.host.instances } returns emptySet()
        every { hostB.provisionedCores } returns 0

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val serverB = mockk<Server>()
        every { serverB.flavor.cpuCount } returns 2
        every { serverB.flavor.memorySize } returns 1024
        every { serverB.meta } returns emptyMap()

        assertEquals(hostA, scheduler.select(serverB))

        every { serverB.meta } returns mapOf("scheduler_hint:different_host" to setOf(serverA.uid))

        assertEquals(hostB, scheduler.select(serverB))
    }

    @Test
    fun testRamWeigher() {
        val scheduler = FilterScheduler(
            filters = emptyList(),
            weighers = listOf(RamWeigher(1.5))
        )

        val hostA = mockk<HostView>()
        every { hostA.host.state } returns HostState.UP
        every { hostA.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.availableMemory } returns 1024

        val hostB = mockk<HostView>()
        every { hostB.host.state } returns HostState.UP
        every { hostB.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.availableMemory } returns 512

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val server = mockk<Server>()
        every { server.flavor.cpuCount } returns 2
        every { server.flavor.memorySize } returns 1024

        assertEquals(hostA, scheduler.select(server))
    }

    @Test
    fun testCoreRamWeigher() {
        val scheduler = FilterScheduler(
            filters = emptyList(),
            weighers = listOf(CoreRamWeigher(1.5))
        )

        val hostA = mockk<HostView>()
        every { hostA.host.state } returns HostState.UP
        every { hostA.host.model } returns HostModel(12 * 2600.0, 12, 2048)
        every { hostA.availableMemory } returns 1024

        val hostB = mockk<HostView>()
        every { hostB.host.state } returns HostState.UP
        every { hostB.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.availableMemory } returns 512

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val server = mockk<Server>()
        every { server.flavor.cpuCount } returns 2
        every { server.flavor.memorySize } returns 1024

        assertEquals(hostB, scheduler.select(server))
    }

    @Test
    fun testVCpuWeigher() {
        val scheduler = FilterScheduler(
            filters = emptyList(),
            weighers = listOf(VCpuWeigher(16.0))
        )

        val hostA = mockk<HostView>()
        every { hostA.host.state } returns HostState.UP
        every { hostA.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.provisionedCores } returns 2

        val hostB = mockk<HostView>()
        every { hostB.host.state } returns HostState.UP
        every { hostB.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.provisionedCores } returns 0

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val server = mockk<Server>()
        every { server.flavor.cpuCount } returns 2
        every { server.flavor.memorySize } returns 1024

        assertEquals(hostB, scheduler.select(server))
    }

    @Test
    fun testInstanceCountWeigher() {
        val scheduler = FilterScheduler(
            filters = emptyList(),
            weighers = listOf(InstanceCountWeigher(multiplier = -1.0))
        )

        val hostA = mockk<HostView>()
        every { hostA.host.state } returns HostState.UP
        every { hostA.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostA.instanceCount } returns 2

        val hostB = mockk<HostView>()
        every { hostB.host.state } returns HostState.UP
        every { hostB.host.model } returns HostModel(4 * 2600.0, 4, 2048)
        every { hostB.instanceCount } returns 0

        scheduler.addHost(hostA)
        scheduler.addHost(hostB)

        val server = mockk<Server>()
        every { server.flavor.cpuCount } returns 2
        every { server.flavor.memorySize } returns 1024

        assertEquals(hostB, scheduler.select(server))
    }
}
