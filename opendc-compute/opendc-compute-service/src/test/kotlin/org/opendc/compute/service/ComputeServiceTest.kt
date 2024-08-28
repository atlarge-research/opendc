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

package org.opendc.compute.service

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.delay
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.compute.api.Flavor
import org.opendc.compute.api.Image
import org.opendc.compute.api.Task
import org.opendc.compute.api.TaskState
import org.opendc.compute.api.TaskWatcher
import org.opendc.compute.service.driver.Host
import org.opendc.compute.service.driver.HostListener
import org.opendc.compute.service.driver.HostModel
import org.opendc.compute.service.driver.HostState
import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.filters.RamFilter
import org.opendc.compute.service.scheduler.filters.VCpuFilter
import org.opendc.compute.service.scheduler.weights.RamWeigher
import org.opendc.simulator.kotlin.SimulationCoroutineScope
import org.opendc.simulator.kotlin.runSimulation
import java.time.Duration
import java.util.UUID

/**
 * Test suite for the [ComputeService] interface.
 */
internal class ComputeServiceTest {
    private lateinit var scope: SimulationCoroutineScope
    private lateinit var service: ComputeService

    @BeforeEach
    fun setUp() {
        scope = SimulationCoroutineScope()
        val computeScheduler =
            FilterScheduler(
                filters = listOf(ComputeFilter(), VCpuFilter(allocationRatio = 1.0), RamFilter(allocationRatio = 1.0)),
                weighers = listOf(RamWeigher()),
            )
        service = ComputeService(scope.dispatcher, computeScheduler, Duration.ofSeconds(30))
    }

    @Test
    fun testClientClose() =
        scope.runSimulation {
            val client = service.newClient()

            assertEquals(emptyList<Flavor>(), client.queryFlavors())
            assertEquals(emptyList<Image>(), client.queryImages())
            assertEquals(emptyList<Task>(), client.queryTasks())

            client.close()

            assertThrows<IllegalStateException> { client.queryFlavors() }
            assertThrows<IllegalStateException> { client.queryImages() }
            assertThrows<IllegalStateException> { client.queryTasks() }

            assertThrows<IllegalStateException> { client.findFlavor(UUID.randomUUID()) }
            assertThrows<IllegalStateException> { client.findImage(UUID.randomUUID()) }
            assertThrows<IllegalStateException> { client.findTask(UUID.randomUUID()) }

            assertThrows<IllegalStateException> { client.newFlavor("test", 1, 2) }
            assertThrows<IllegalStateException> { client.newImage("test") }
            assertThrows<IllegalStateException> { client.newTask("test", mockk(), mockk()) }
        }

    @Test
    fun testClientCreate() =
        scope.runSimulation {
            val client = service.newClient()

            val flavor = client.newFlavor("test", 1, 1024)
            assertEquals(listOf(flavor), client.queryFlavors())
            assertEquals(flavor, client.findFlavor(flavor.uid))
            val image = client.newImage("test")
            assertEquals(listOf(image), client.queryImages())
            assertEquals(image, client.findImage(image.uid))
            val server = client.newTask("test", image, flavor, start = false)
            assertEquals(listOf(server), client.queryTasks())
            assertEquals(server, client.findTask(server.uid))

            server.delete()
            assertNull(client.findTask(server.uid))

            image.delete()
            assertNull(client.findImage(image.uid))

            flavor.delete()
            assertNull(client.findFlavor(flavor.uid))

            assertThrows<IllegalStateException> { server.start() }
        }

    @Test
    fun testClientOnClose() =
        scope.runSimulation {
            service.close()
            assertThrows<IllegalStateException> {
                service.newClient()
            }
        }

    @Test
    fun testAddHost() =
        scope.runSimulation {
            val host = mockk<Host>(relaxUnitFun = true)

            every { host.model } returns HostModel(4 * 2600.0, 1, 4, 2048)
            every { host.state } returns HostState.UP

            assertEquals(emptySet<Host>(), service.hosts)

            service.addHost(host)

            verify(exactly = 1) { host.addListener(any()) }

            assertEquals(1, service.hosts.size)

            service.removeHost(host)

            verify(exactly = 1) { host.removeListener(any()) }
        }

    @Test
    fun testAddHostDouble() =
        scope.runSimulation {
            val host = mockk<Host>(relaxUnitFun = true)

            every { host.model } returns HostModel(4 * 2600.0, 1, 4, 2048)
            every { host.state } returns HostState.DOWN

            assertEquals(emptySet<Host>(), service.hosts)

            service.addHost(host)
            service.addHost(host)

            verify(exactly = 1) { host.addListener(any()) }
        }

    @Test
    fun testServerStartWithoutEnoughCpus() =
        scope.runSimulation {
            val client = service.newClient()
            val flavor = client.newFlavor("test", 1, 0)
            val image = client.newImage("test")
            val server = client.newTask("test", image, flavor, start = false)

            server.start()
            delay(5L * 60 * 1000)
            server.reload()
            assertEquals(TaskState.TERMINATED, server.state)
        }

    @Test
    fun testServerStartWithoutEnoughMemory() =
        scope.runSimulation {
            val client = service.newClient()
            val flavor = client.newFlavor("test", 0, 1024)
            val image = client.newImage("test")
            val server = client.newTask("test", image, flavor, start = false)

            server.start()
            delay(5L * 60 * 1000)
            server.reload()
            assertEquals(TaskState.TERMINATED, server.state)
        }

    @Test
    fun testServerStartWithoutEnoughResources() =
        scope.runSimulation {
            val client = service.newClient()
            val flavor = client.newFlavor("test", 1, 1024)
            val image = client.newImage("test")
            val server = client.newTask("test", image, flavor, start = false)

            server.start()
            delay(5L * 60 * 1000)
            server.reload()
            assertEquals(TaskState.TERMINATED, server.state)
        }

    @Test
    fun testServerCancelRequest() =
        scope.runSimulation {
            val client = service.newClient()
            val flavor = client.newFlavor("test", 1, 1024)
            val image = client.newImage("test")
            val server = client.newTask("test", image, flavor, start = false)

            server.start()
            server.stop()
            delay(5L * 60 * 1000)
            server.reload()
            assertEquals(TaskState.TERMINATED, server.state)
        }

    @Test
    fun testServerCannotFitOnHost() =
        scope.runSimulation {
            val host = mockk<Host>(relaxUnitFun = true)

            every { host.model } returns HostModel(4 * 2600.0, 1, 4, 2048)
            every { host.state } returns HostState.UP
            every { host.canFit(any()) } returns false

            service.addHost(host)

            val client = service.newClient()
            val flavor = client.newFlavor("test", 1, 1024)
            val image = client.newImage("test")
            val server = client.newTask("test", image, flavor, start = false)

            server.start()
            delay(10L * 60 * 1000)
            server.reload()
            assertEquals(TaskState.PROVISIONING, server.state)

            verify { host.canFit(server) }
        }

    @Test
    fun testHostAvailableAfterSomeTime() =
        scope.runSimulation {
            val host = mockk<Host>(relaxUnitFun = true)
            val listeners = mutableListOf<HostListener>()

            every { host.uid } returns UUID.randomUUID()
            every { host.model } returns HostModel(4 * 2600.0, 1, 4, 2048)
            every { host.state } returns HostState.DOWN
            every { host.addListener(any()) } answers { listeners.add(it.invocation.args[0] as HostListener) }
            every { host.canFit(any()) } returns false

            service.addHost(host)

            val client = service.newClient()
            val flavor = client.newFlavor("test", 1, 1024)
            val image = client.newImage("test")
            val server = client.newTask("test", image, flavor, start = false)

            server.start()
            delay(5L * 60 * 1000)

            every { host.state } returns HostState.UP
            listeners.forEach { it.onStateChanged(host, HostState.UP) }

            delay(5L * 60 * 1000)
            server.reload()
            assertEquals(TaskState.PROVISIONING, server.state)

            verify { host.canFit(server) }
        }

    @Test
    fun testHostUnavailableAfterSomeTime() =
        scope.runSimulation {
            val host = mockk<Host>(relaxUnitFun = true)
            val listeners = mutableListOf<HostListener>()

            every { host.uid } returns UUID.randomUUID()
            every { host.model } returns HostModel(4 * 2600.0, 1, 4, 2048)
            every { host.state } returns HostState.UP
            every { host.addListener(any()) } answers { listeners.add(it.invocation.args[0] as HostListener) }
            every { host.canFit(any()) } returns false

            service.addHost(host)

            val client = service.newClient()
            val flavor = client.newFlavor("test", 1, 1024)
            val image = client.newImage("test")
            val server = client.newTask("test", image, flavor, start = false)

            delay(5L * 60 * 1000)

            every { host.state } returns HostState.DOWN
            listeners.forEach { it.onStateChanged(host, HostState.DOWN) }

            server.start()
            delay(5L * 60 * 1000)
            server.reload()
            assertEquals(TaskState.PROVISIONING, server.state)

            verify(exactly = 0) { host.canFit(server) }
        }

    @Test
    fun testServerDeploy() =
        scope.runSimulation {
            val host = mockk<Host>(relaxUnitFun = true)
            val listeners = mutableListOf<HostListener>()

            every { host.uid } returns UUID.randomUUID()
            every { host.model } returns HostModel(4 * 2600.0, 1, 4, 2048)
            every { host.state } returns HostState.UP
            every { host.canFit(any()) } returns true
            every { host.addListener(any()) } answers { listeners.add(it.invocation.args[0] as HostListener) }

            service.addHost(host)

            val client = service.newClient()
            val flavor = client.newFlavor("test", 1, 1024)
            val image = client.newImage("test")
            val server = client.newTask("test", image, flavor, start = false)
            val slot = slot<Task>()

            val watcher = mockk<TaskWatcher>(relaxUnitFun = true)
            server.watch(watcher)

            // Start server
            server.start()
            delay(5L * 60 * 1000)
            coVerify { host.spawn(capture(slot)) }

            listeners.forEach { it.onStateChanged(host, slot.captured, TaskState.RUNNING) }

            server.reload()
            assertEquals(TaskState.RUNNING, server.state)

            verify { watcher.onStateChanged(server, TaskState.RUNNING) }

            // Stop server
            listeners.forEach { it.onStateChanged(host, slot.captured, TaskState.TERMINATED) }

            server.reload()
            assertEquals(TaskState.TERMINATED, server.state)

            verify { watcher.onStateChanged(server, TaskState.TERMINATED) }
        }

    @Test
    fun testServerDeployFailure() =
        scope.runSimulation {
            val host = mockk<Host>(relaxUnitFun = true)
            val listeners = mutableListOf<HostListener>()

            every { host.uid } returns UUID.randomUUID()
            every { host.model } returns HostModel(4 * 2600.0, 1, 4, 2048)
            every { host.state } returns HostState.UP
            every { host.canFit(any()) } returns true
            every { host.addListener(any()) } answers { listeners.add(it.invocation.args[0] as HostListener) }
            coEvery { host.spawn(any()) } throws IllegalStateException()

            service.addHost(host)

            val client = service.newClient()
            val flavor = client.newFlavor("test", 1, 1024)
            val image = client.newImage("test")
            val server = client.newTask("test", image, flavor, start = false)

            server.start()
            delay(5L * 60 * 1000)

            server.reload()
            assertEquals(TaskState.PROVISIONING, server.state)
        }
}
