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
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, mockk(), mockk())

        val client = assertDoesNotThrow { service.newClient() }
        assertDoesNotThrow { client.close() }

        assertThrows<IllegalStateException> { client.queryFunctions() }
        assertThrows<IllegalStateException> { client.newFunction("test") }
        assertThrows<IllegalStateException> { client.invoke("test") }
        assertThrows<IllegalStateException> { client.findFunction(UUID.randomUUID()) }
        assertThrows<IllegalStateException> { client.findFunction("name") }
    }

    @Test
    fun testClientInvokeUnknown() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, mockk(), mockk())

        val client = service.newClient()

        assertThrows<IllegalArgumentException> { client.invoke("test") }
    }

    @Test
    fun testClientFunctionCreation() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, mockk(), mockk())

        val client = service.newClient()

        val function = client.newFunction("test")

        assertEquals("test", function.name)
    }

    @Test
    fun testClientFunctionQuery() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, mockk(), mockk())

        val client = service.newClient()

        assertEquals(emptyList<ServerlessFunction>(), client.queryFunctions())

        val function = client.newFunction("test")

        assertEquals(listOf(function), client.queryFunctions())
    }

    @Test
    fun testClientFunctionFindById() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, mockk(), mockk())

        val client = service.newClient()

        assertEquals(emptyList<ServerlessFunction>(), client.queryFunctions())

        val function = client.newFunction("test")

        assertNotNull(client.findFunction(function.uid))
    }

    @Test
    fun testClientFunctionFindByName() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, mockk(), mockk())

        val client = service.newClient()

        assertEquals(emptyList<ServerlessFunction>(), client.queryFunctions())

        val function = client.newFunction("test")

        assertNotNull(client.findFunction(function.name))
    }

    @Test
    fun testClientFunctionDuplicateName() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, mockk(), mockk())

        val client = service.newClient()

        client.newFunction("test")

        assertThrows<IllegalArgumentException> { client.newFunction("test") }
    }

    @Test
    fun testClientFunctionDelete() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, mockk(), mockk())

        val client = service.newClient()
        val function = client.newFunction("test")
        assertNotNull(client.findFunction(function.uid))
        function.delete()
        assertNull(client.findFunction(function.uid))

        // Delete should be idempotent
        function.delete()
    }

    @Test
    fun testClientFunctionCannotInvokeDeleted() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val service = ServerlessService(coroutineContext, clock, mockk(), mockk())

        val client = service.newClient()
        val function = client.newFunction("test")
        assertNotNull(client.findFunction(function.uid))
        function.delete()

        assertThrows<IllegalStateException> { function.invoke() }
    }

    @Test
    fun testClientFunctionInvoke() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val deployer = mockk<FunctionDeployer>()
        val service = ServerlessService(coroutineContext, clock, deployer, mockk())

        every { deployer.deploy(any()) } answers {
            object : FunctionInstance {
                override val state: FunctionInstanceState = FunctionInstanceState.Idle
                override val function: ServerlessFunction = it.invocation.args[0] as ServerlessFunction

                override suspend fun invoke() {}

                override fun close() {}
            }
        }

        val client = service.newClient()
        val function = client.newFunction("test")

        function.invoke()
    }
}
