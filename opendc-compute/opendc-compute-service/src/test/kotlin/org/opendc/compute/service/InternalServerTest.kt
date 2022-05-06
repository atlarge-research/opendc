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

import io.mockk.*
import kotlinx.coroutines.yield
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.compute.api.Server
import org.opendc.compute.api.ServerState
import org.opendc.compute.service.driver.Host
import org.opendc.compute.service.internal.ComputeServiceImpl
import org.opendc.compute.service.internal.InternalFlavor
import org.opendc.compute.service.internal.InternalImage
import org.opendc.compute.service.internal.InternalServer
import org.opendc.simulator.core.runBlockingSimulation
import java.util.*

/**
 * Test suite for the [InternalServer] implementation.
 */
class InternalServerTest {
    @Test
    fun testEquality() {
        val service = mockk<ComputeServiceImpl>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()

        val a = InternalServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf())
        val b = InternalServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf())

        assertEquals(a, b)
    }

    @Test
    fun testEqualityWithDifferentType() {
        val service = mockk<ComputeServiceImpl>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val a = InternalServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf())

        val b = mockk<Server>(relaxUnitFun = true)
        every { b.uid } returns uid

        assertEquals(a, b)
    }

    @Test
    fun testInequalityWithDifferentType() {
        val service = mockk<ComputeServiceImpl>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val a = InternalServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf())

        val b = mockk<Server>(relaxUnitFun = true)
        every { b.uid } returns UUID.randomUUID()

        assertNotEquals(a, b)
    }

    @Test
    fun testInequalityWithIncorrectType() {
        val service = mockk<ComputeServiceImpl>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val a = InternalServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf())

        assertNotEquals(a, Unit)
    }

    @Test
    fun testStartTerminatedServer() = runBlockingSimulation {
        val service = mockk<ComputeServiceImpl>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = InternalServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf())

        every { service.schedule(any()) } answers { ComputeServiceImpl.SchedulingRequest(it.invocation.args[0] as InternalServer, 0) }

        server.start()

        verify(exactly = 1) { service.schedule(server) }
        assertEquals(ServerState.PROVISIONING, server.state)
    }

    @Test
    fun testStartDeletedServer() = runBlockingSimulation {
        val service = mockk<ComputeServiceImpl>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = InternalServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf())

        server.state = ServerState.DELETED

        assertThrows<IllegalStateException> { server.start() }
    }

    @Test
    fun testStartProvisioningServer() = runBlockingSimulation {
        val service = mockk<ComputeServiceImpl>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = InternalServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf())

        server.state = ServerState.PROVISIONING

        server.start()

        assertEquals(ServerState.PROVISIONING, server.state)
    }

    @Test
    fun testStartRunningServer() = runBlockingSimulation {
        val service = mockk<ComputeServiceImpl>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = InternalServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf())

        server.state = ServerState.RUNNING

        server.start()

        assertEquals(ServerState.RUNNING, server.state)
    }

    @Test
    fun testStopProvisioningServer() = runBlockingSimulation {
        val service = mockk<ComputeServiceImpl>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = InternalServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf())
        val request = ComputeServiceImpl.SchedulingRequest(server, 0)

        every { service.schedule(any()) } returns request

        server.start()
        server.stop()

        assertTrue(request.isCancelled)
        assertEquals(ServerState.TERMINATED, server.state)
    }

    @Test
    fun testStopTerminatedServer() = runBlockingSimulation {
        val service = mockk<ComputeServiceImpl>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = InternalServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf())

        server.state = ServerState.TERMINATED
        server.stop()

        assertEquals(ServerState.TERMINATED, server.state)
    }

    @Test
    fun testStopDeletedServer() = runBlockingSimulation {
        val service = mockk<ComputeServiceImpl>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = InternalServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf())

        server.state = ServerState.DELETED
        server.stop()

        assertEquals(ServerState.DELETED, server.state)
    }

    @Test
    fun testStopRunningServer() = runBlockingSimulation {
        val service = mockk<ComputeServiceImpl>()
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = InternalServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf())
        val host = mockk<Host>(relaxUnitFun = true)

        server.state = ServerState.RUNNING
        server.host = host
        server.stop()
        yield()

        coVerify { host.stop(server) }
    }

    @Test
    fun testDeleteProvisioningServer() = runBlockingSimulation {
        val service = mockk<ComputeServiceImpl>(relaxUnitFun = true)
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = InternalServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf())
        val request = ComputeServiceImpl.SchedulingRequest(server, 0)

        every { service.schedule(any()) } returns request

        server.start()
        server.delete()

        assertTrue(request.isCancelled)
        assertEquals(ServerState.DELETED, server.state)
        verify { service.delete(server) }
    }

    @Test
    fun testDeleteTerminatedServer() = runBlockingSimulation {
        val service = mockk<ComputeServiceImpl>(relaxUnitFun = true)
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = InternalServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf())

        server.state = ServerState.TERMINATED
        server.delete()

        assertEquals(ServerState.DELETED, server.state)

        verify { service.delete(server) }
    }

    @Test
    fun testDeleteDeletedServer() = runBlockingSimulation {
        val service = mockk<ComputeServiceImpl>(relaxUnitFun = true)
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = InternalServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf())

        server.state = ServerState.DELETED
        server.delete()

        assertEquals(ServerState.DELETED, server.state)
    }

    @Test
    fun testDeleteRunningServer() = runBlockingSimulation {
        val service = mockk<ComputeServiceImpl>(relaxUnitFun = true)
        val uid = UUID.randomUUID()
        val flavor = mockFlavor()
        val image = mockImage()
        val server = InternalServer(service, uid, "test", flavor, image, mutableMapOf(), mutableMapOf())
        val host = mockk<Host>(relaxUnitFun = true)

        server.state = ServerState.RUNNING
        server.host = host
        server.delete()
        yield()

        coVerify { host.delete(server) }
        verify { service.delete(server) }
    }

    private fun mockFlavor(): InternalFlavor {
        val flavor = mockk<InternalFlavor>()
        every { flavor.name } returns "c5.large"
        every { flavor.uid } returns UUID.randomUUID()
        every { flavor.cpuCount } returns 2
        every { flavor.memorySize } returns 4096
        return flavor
    }

    private fun mockImage(): InternalImage {
        val image = mockk<InternalImage>()
        every { image.name } returns "ubuntu-20.04"
        every { image.uid } returns UUID.randomUUID()
        return image
    }
}
