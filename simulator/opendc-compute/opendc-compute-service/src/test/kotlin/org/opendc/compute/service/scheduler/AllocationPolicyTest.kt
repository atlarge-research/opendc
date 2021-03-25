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
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.opendc.compute.api.Server
import org.opendc.compute.service.internal.HostView
import java.util.*
import java.util.stream.Stream
import kotlin.random.Random

/**
 * Test suite for the [AllocationPolicy] interface.
 */
internal class AllocationPolicyTest {
    @ParameterizedTest
    @MethodSource("activeServersArgs")
    fun testActiveServersPolicy(
        reversed: Boolean,
        hosts: Set<HostView>,
        server: Server,
        expectedHost: HostView?
    ) {
        val policy = NumberOfActiveServersAllocationPolicy(reversed)
        assertEquals(expectedHost, policy.invoke().select(hosts, server))
    }

    @ParameterizedTest
    @MethodSource("availableMemoryArgs")
    fun testAvailableMemoryPolicy(
        reversed: Boolean,
        hosts: Set<HostView>,
        server: Server,
        expectedHost: HostView?
    ) {
        val policy = AvailableMemoryAllocationPolicy(reversed)
        assertEquals(expectedHost, policy.invoke().select(hosts, server))
    }

    @ParameterizedTest
    @MethodSource("availableCoreMemoryArgs")
    fun testAvailableCoreMemoryPolicy(
        reversed: Boolean,
        hosts: Set<HostView>,
        server: Server,
        expectedHost: HostView?
    ) {
        val policy = AvailableMemoryAllocationPolicy(reversed)
        assertEquals(expectedHost, policy.invoke().select(hosts, server))
    }

    @ParameterizedTest
    @MethodSource("provisionedCoresArgs")
    fun testProvisionedPolicy(
        reversed: Boolean,
        hosts: Set<HostView>,
        server: Server,
        expectedHost: HostView?
    ) {
        val policy = ProvisionedCoresAllocationPolicy(reversed)
        assertEquals(expectedHost, policy.invoke().select(hosts, server))
    }

    @Suppress("unused")
    private companion object {
        /**
         * Test arguments for the [NumberOfActiveServersAllocationPolicy].
         */
        @JvmStatic
        fun activeServersArgs(): Stream<Arguments> {
            val random = Random(1)
            val hosts = List(4) { i ->
                val view = mockk<HostView>()
                every { view.host.uid } returns UUID(0, i.toLong())
                every { view.host.model.cpuCount } returns random.nextInt(1, 16)
                every { view.host.model.memorySize } returns random.nextLong(1024, 1024 * 1024)
                every { view.availableMemory } returns random.nextLong(0, view.host.model.memorySize)
                every { view.numberOfActiveServers } returns random.nextInt(0, 6)
                every { view.provisionedCores } returns random.nextInt(0, view.host.model.cpuCount)
                every { view.toString() } returns "HostView[$i,numberOfActiveServers=${view.numberOfActiveServers}]"
                view
            }

            val servers = List(2) {
                val server = mockk<Server>()
                every { server.flavor.cpuCount } returns random.nextInt(1, 8)
                every { server.flavor.memorySize } returns random.nextLong(1024, 1024 * 512)
                server
            }

            return Stream.of(
                Arguments.of(false, hosts.toSet(), servers[0], hosts[2]),
                Arguments.of(false, hosts.toSet(), servers[1], hosts[1]),
                Arguments.of(true, hosts.toSet(), servers[1], hosts[0]),
            )
        }

        /**
         * Test arguments for the [AvailableCoreMemoryAllocationPolicy].
         */
        @JvmStatic
        fun availableCoreMemoryArgs(): Stream<Arguments> {
            val random = Random(1)
            val hosts = List(4) { i ->
                val view = mockk<HostView>()
                every { view.host.uid } returns UUID(0, i.toLong())
                every { view.host.model.cpuCount } returns random.nextInt(1, 16)
                every { view.host.model.memorySize } returns random.nextLong(1024, 1024 * 1024)
                every { view.availableMemory } returns random.nextLong(0, view.host.model.memorySize)
                every { view.numberOfActiveServers } returns random.nextInt(0, 6)
                every { view.provisionedCores } returns random.nextInt(0, view.host.model.cpuCount)
                every { view.toString() } returns "HostView[$i,availableMemory=${view.availableMemory}]"
                view
            }

            val servers = List(2) {
                val server = mockk<Server>()
                every { server.flavor.cpuCount } returns random.nextInt(1, 8)
                every { server.flavor.memorySize } returns random.nextLong(1024, 1024 * 512)
                server
            }

            return Stream.of(
                Arguments.of(false, hosts.toSet(), servers[0], hosts[2]),
                Arguments.of(false, hosts.toSet(), servers[1], hosts[2]),
                Arguments.of(true, hosts.toSet(), servers[1], hosts[1]),
            )
        }

        /**
         * Test arguments for the [AvailableMemoryAllocationPolicy].
         */
        @JvmStatic
        fun availableMemoryArgs(): Stream<Arguments> {
            val random = Random(1)
            val hosts = List(4) { i ->
                val view = mockk<HostView>()
                every { view.host.uid } returns UUID(0, i.toLong())
                every { view.host.model.cpuCount } returns random.nextInt(1, 16)
                every { view.host.model.memorySize } returns random.nextLong(1024, 1024 * 1024)
                every { view.availableMemory } returns random.nextLong(0, view.host.model.memorySize)
                every { view.numberOfActiveServers } returns random.nextInt(0, 6)
                every { view.provisionedCores } returns random.nextInt(0, view.host.model.cpuCount)
                every { view.toString() } returns "HostView[$i,availableMemory=${view.availableMemory}]"
                view
            }

            val servers = List(2) {
                val server = mockk<Server>()
                every { server.flavor.cpuCount } returns random.nextInt(1, 8)
                every { server.flavor.memorySize } returns random.nextLong(1024, 1024 * 512)
                server
            }

            return Stream.of(
                Arguments.of(false, hosts.toSet(), servers[0], hosts[2]),
                Arguments.of(false, hosts.toSet(), servers[1], hosts[2]),
                Arguments.of(true, hosts.toSet(), servers[1], hosts[1]),
            )
        }

        /**
         * Test arguments for the [ProvisionedCoresAllocationPolicy].
         */
        @JvmStatic
        fun provisionedCoresArgs(): Stream<Arguments> {
            val random = Random(1)
            val hosts = List(4) { i ->
                val view = mockk<HostView>()
                every { view.host.uid } returns UUID(0, i.toLong())
                every { view.host.model.cpuCount } returns random.nextInt(1, 16)
                every { view.host.model.memorySize } returns random.nextLong(1024, 1024 * 1024)
                every { view.availableMemory } returns random.nextLong(0, view.host.model.memorySize)
                every { view.numberOfActiveServers } returns random.nextInt(0, 6)
                every { view.provisionedCores } returns random.nextInt(0, view.host.model.cpuCount)
                every { view.toString() } returns "HostView[$i,provisionedCores=${view.provisionedCores}]"
                view
            }

            val servers = List(2) {
                val server = mockk<Server>()
                every { server.flavor.cpuCount } returns random.nextInt(1, 8)
                every { server.flavor.memorySize } returns random.nextLong(1024, 1024 * 512)
                server
            }

            return Stream.of(
                Arguments.of(false, hosts.toSet(), servers[0], hosts[2]),
                Arguments.of(false, hosts.toSet(), servers[1], hosts[0]),
                Arguments.of(true, hosts.toSet(), servers[1], hosts[0]),
            )
        }
    }
}
