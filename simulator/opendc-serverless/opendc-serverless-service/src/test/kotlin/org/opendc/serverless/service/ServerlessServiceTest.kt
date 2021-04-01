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

package org.opendc.serverless.service

import io.mockk.*
import io.opentelemetry.api.metrics.MeterProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.opendc.serverless.api.ServerlessFunction
import org.opendc.serverless.service.deployer.FunctionDeployer
import org.opendc.serverless.service.deployer.FunctionInstance
import org.opendc.serverless.service.deployer.FunctionInstanceState
import org.opendc.simulator.utils.DelayControllerClockAdapter
import java.util.*

/**
 * Test suite for the [ServerlessService] implementation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class ServerlessServiceTest {

    @Test
    fun testClientState() = runBlockingTest {
        val meter = MeterProvider.noop().get("opendc-serverless")
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, meter, mockk(), mockk())

        val client = assertDoesNotThrow { service.newClient() }
        assertDoesNotThrow { client.close() }

        assertThrows<IllegalStateException> { client.queryFunctions() }
        assertThrows<IllegalStateException> { client.newFunction("test", 128) }
        assertThrows<IllegalStateException> { client.invoke("test") }
        assertThrows<IllegalStateException> { client.findFunction(UUID.randomUUID()) }
        assertThrows<IllegalStateException> { client.findFunction("name") }
    }

    @Test
    fun testClientInvokeUnknown() = runBlockingTest {
        val meter = MeterProvider.noop().get("opendc-serverless")
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, meter, mockk(), mockk())

        val client = service.newClient()

        assertThrows<IllegalArgumentException> { client.invoke("test") }
    }

    @Test
    fun testClientFunctionCreation() = runBlockingTest {
        val meter = MeterProvider.noop().get("opendc-serverless")
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, meter, mockk(), mockk())

        val client = service.newClient()

        val function = client.newFunction("test", 128)

        assertEquals("test", function.name)
    }

    @Test
    fun testClientFunctionQuery() = runBlockingTest {
        val meter = MeterProvider.noop().get("opendc-serverless")
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, meter, mockk(), mockk())

        val client = service.newClient()

        assertEquals(emptyList<ServerlessFunction>(), client.queryFunctions())

        val function = client.newFunction("test", 128)

        assertEquals(listOf(function), client.queryFunctions())
    }

    @Test
    fun testClientFunctionFindById() = runBlockingTest {
        val meter = MeterProvider.noop().get("opendc-serverless")
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, meter, mockk(), mockk())

        val client = service.newClient()

        assertEquals(emptyList<ServerlessFunction>(), client.queryFunctions())

        val function = client.newFunction("test", 128)

        assertNotNull(client.findFunction(function.uid))
    }

    @Test
    fun testClientFunctionFindByName() = runBlockingTest {
        val meter = MeterProvider.noop().get("opendc-serverless")
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, meter, mockk(), mockk())

        val client = service.newClient()

        assertEquals(emptyList<ServerlessFunction>(), client.queryFunctions())

        val function = client.newFunction("test", 128)

        assertNotNull(client.findFunction(function.name))
    }

    @Test
    fun testClientFunctionDuplicateName() = runBlockingTest {
        val meter = MeterProvider.noop().get("opendc-serverless")
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, meter, mockk(), mockk())

        val client = service.newClient()

        client.newFunction("test", 128)

        assertThrows<IllegalArgumentException> { client.newFunction("test", 128) }
    }

    @Test
    fun testClientFunctionDelete() = runBlockingTest {
        val meter = MeterProvider.noop().get("opendc-serverless")
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, meter, mockk(), mockk())

        val client = service.newClient()
        val function = client.newFunction("test", 128)
        assertNotNull(client.findFunction(function.uid))
        function.delete()
        assertNull(client.findFunction(function.uid))

        // Delete should be idempotent
        function.delete()
    }

    @Test
    fun testClientFunctionCannotInvokeDeleted() = runBlockingTest {
        val meter = MeterProvider.noop().get("opendc-serverless")
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, meter, mockk(), mockk())

        val client = service.newClient()
        val function = client.newFunction("test", 128)
        assertNotNull(client.findFunction(function.uid))
        function.delete()

        assertThrows<IllegalStateException> { function.invoke() }
    }

    @Test
    fun testClientFunctionInvoke() = runBlockingTest {
        val meter = MeterProvider.noop().get("opendc-serverless")
        val clock = DelayControllerClockAdapter(this)
        val deployer = mockk<FunctionDeployer>()
        val service = ServerlessService(coroutineContext, clock, meter, deployer, mockk())

        every { deployer.deploy(any()) } answers {
            object : FunctionInstance {
                override val state: FunctionInstanceState = FunctionInstanceState.Idle
                override val function: FunctionObject = it.invocation.args[0] as FunctionObject

                override suspend fun invoke() {}

                override fun close() {}
            }
        }

        val client = service.newClient()
        val function = client.newFunction("test", 128)

        function.invoke()
    }
}
