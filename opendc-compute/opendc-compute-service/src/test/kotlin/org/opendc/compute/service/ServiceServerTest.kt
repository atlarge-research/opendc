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

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.compute.api.Server
import org.opendc.compute.api.ServerState
import org.opendc.compute.service.driver.Host
import org.opendc.simulator.kotlin.runSimulation
import java.util.UUID

/**
 * Test suite for the [ServiceServer] implementation.
 */
class ServiceServerTest {
    @Test
    fun testEquality() {
        val service = mockk<ComputeService>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()

        val a = ServiceServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf<String, Any>())
        val b = ServiceServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf<String, Any>())

        assertEquals(a, b)
    }

    @Test
    fun testInequalityWithDifferentType() {
        val service = mockk<ComputeService>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val a = ServiceServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf<String, Any>())

        val b = mockk<Server>(relaxUnitFun = true)
        every { b.uid } returns UUID.randomUUID()

        assertNotEquals(a, b)
    }

    @Test
    fun testInequalityWithIncorrectType() {
        val service = mockk<ComputeService>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val a = ServiceServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf<String, Any>())

        assertNotEquals(a, Unit)
    }

    @Test
    fun testStartTerminatedServer() = runSimulation {
        val service = mockk<ComputeService>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = ServiceServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf<String, Any>())

        every { service.schedule(any()) } answers { ComputeService.SchedulingRequest(it.invocation.args[0] as ServiceServer, 0) }

        server.start()

        verify(exactly = 1) { service.schedule(server) }
        assertEquals(ServerState.PROVISIONING, server.state)
    }

    @Test
    fun testStartDeletedServer() = runSimulation {
        val service = mockk<ComputeService>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = ServiceServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf<String, Any>())

        server.setState(ServerState.DELETED)

        assertThrows<IllegalStateException> { server.start() }
    }

    @Test
    fun testStartProvisioningServer() = runSimulation {
        val service = mockk<ComputeService>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = ServiceServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf<String, Any>())

        server.setState(ServerState.PROVISIONING)

        server.start()

        assertEquals(ServerState.PROVISIONING, server.state)
    }

    @Test
    fun testStartRunningServer() = runSimulation {
        val service = mockk<ComputeService>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = ServiceServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf<String, Any>())

        server.setState(ServerState.RUNNING)

        server.start()

        assertEquals(ServerState.RUNNING, server.state)
    }

    @Test
    fun testStopProvisioningServer() = runSimulation {
        val service = mockk<ComputeService>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = ServiceServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf<String, Any>())
        val request = ComputeService.SchedulingRequest(server, 0)

        every { service.schedule(any()) } returns request

        server.start()
        server.stop()

        assertTrue(request.isCancelled)
        assertEquals(ServerState.TERMINATED, server.state)
    }

    @Test
    fun testStopTerminatedServer() = runSimulation {
        val service = mockk<ComputeService>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = ServiceServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf<String, Any>())

        server.setState(ServerState.TERMINATED)
        server.stop()

        assertEquals(ServerState.TERMINATED, server.state)
    }

    @Test
    fun testStopDeletedServer() = runSimulation {
        val service = mockk<ComputeService>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = ServiceServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf<String, Any>())

        server.setState(ServerState.DELETED)
        server.stop()

        assertEquals(ServerState.DELETED, server.state)
    }

    @Test
    fun testStopRunningServer() = runSimulation {
        val service = mockk<ComputeService>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = ServiceServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf<String, Any>())
        val host = mockk<Host>(relaxUnitFun = true)

        server.setState(ServerState.RUNNING)
        server.host = host
        server.stop()
        yield()

        verify { host.stop(server) }
    }

    @Test
    fun testDeleteProvisioningServer() = runSimulation {
        val service = mockk<ComputeService>(relaxUnitFun = true)
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = ServiceServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf<String, Any>())
        val request = ComputeService.SchedulingRequest(server, 0)

        every { service.schedule(any()) } returns request

        server.start()
        server.delete()

        assertTrue(request.isCancelled)
        assertEquals(ServerState.DELETED, server.state)
        verify { service.delete(server) }
    }

    @Test
    fun testDeleteTerminatedServer() = runSimulation {
        val service = mockk<ComputeService>(relaxUnitFun = true)
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = ServiceServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf<String, Any>())

        server.setState(ServerState.TERMINATED)
        server.delete()

        assertEquals(ServerState.DELETED, server.state)

        verify { service.delete(server) }
    }

    @Test
    fun testDeleteDeletedServer() = runSimulation {
        val service = mockk<ComputeService>(relaxUnitFun = true)
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = ServiceServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf<String, Any>())

        server.setState(ServerState.DELETED)
        server.delete()

        assertEquals(ServerState.DELETED, server.state)
    }

    @Test
    fun testDeleteRunningServer() = runSimulation {
        val service = mockk<ComputeService>(relaxUnitFun = true)
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = ServiceServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf<String, Any>())
        val host = mockk<Host>(relaxUnitFun = true)

        server.setState(ServerState.RUNNING)
        server.host = host
        server.delete()
        yield()

        verify { host.delete(server) }
        verify { service.delete(server) }
    }

    private fun mockFlavor(): ServiceFlavor {
        val flavor = mockk<ServiceFlavor>()
        every { flavor.name } returns "c5.large"
        every { flavor.uid } returns UUID.randomUUID()
        every { flavor.cpuCount } returns 2
        every { flavor.memorySize } returns 4096
        return flavor
    }

    private fun mockImage(): ServiceImage {
        val image = mockk<ServiceImage>()
        every { image.name } returns "ubuntu-20.04"
        every { image.uid } returns UUID.randomUUID()
        return image
    }
}
