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

package org.opendc.faas.service

import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.opendc.faas.api.FaaSFunction
import org.opendc.faas.service.deployer.FunctionDeployer
import org.opendc.faas.service.deployer.FunctionInstance
import org.opendc.faas.service.deployer.FunctionInstanceState
import org.opendc.simulator.kotlin.runSimulation
import java.util.UUID

/**
 * Test suite for the [FaaSService] implementation.
 */
internal class FaaSServiceTest {

    @Test
    fun testClientState() = runSimulation {
        val service = FaaSService(dispatcher, mockk(), mockk(), mockk())

        val client = assertDoesNotThrow { service.newClient() }
        assertDoesNotThrow { client.close() }

        assertThrows<IllegalStateException> { client.queryFunctions() }
        assertThrows<IllegalStateException> { client.newFunction("test", 128) }
        assertThrows<IllegalStateException> { client.invoke("test") }
        assertThrows<IllegalStateException> { client.findFunction(UUID.randomUUID()) }
        assertThrows<IllegalStateException> { client.findFunction("name") }
    }

    @Test
    fun testClientInvokeUnknown() = runSimulation {
        val service = FaaSService(dispatcher, mockk(), mockk(), mockk())

        val client = service.newClient()

        assertThrows<IllegalArgumentException> { client.invoke("test") }
    }

    @Test
    fun testClientFunctionCreation() = runSimulation {
        val service = FaaSService(dispatcher, mockk(), mockk(), mockk())

        val client = service.newClient()

        val function = client.newFunction("test", 128)

        assertEquals("test", function.name)
    }

    @Test
    fun testClientFunctionQuery() = runSimulation {
        val service = FaaSService(dispatcher, mockk(), mockk(), mockk())

        val client = service.newClient()

        assertEquals(emptyList<FaaSFunction>(), client.queryFunctions())

        val function = client.newFunction("test", 128)

        assertEquals(listOf(function), client.queryFunctions())
    }

    @Test
    fun testClientFunctionFindById() = runSimulation {
        val service = FaaSService(dispatcher, mockk(), mockk(), mockk())

        val client = service.newClient()

        assertEquals(emptyList<FaaSFunction>(), client.queryFunctions())

        val function = client.newFunction("test", 128)

        assertNotNull(client.findFunction(function.uid))
    }

    @Test
    fun testClientFunctionFindByName() = runSimulation {
        val service = FaaSService(dispatcher, mockk(), mockk(), mockk())

        val client = service.newClient()

        assertEquals(emptyList<FaaSFunction>(), client.queryFunctions())

        val function = client.newFunction("test", 128)

        assertNotNull(client.findFunction(function.name))
    }

    @Test
    fun testClientFunctionDuplicateName() = runSimulation {
        val service = FaaSService(dispatcher, mockk(), mockk(), mockk())

        val client = service.newClient()

        client.newFunction("test", 128)

        assertThrows<IllegalArgumentException> { client.newFunction("test", 128) }
    }

    @Test
    fun testClientFunctionDelete() = runSimulation {
        val service = FaaSService(dispatcher, mockk(), mockk(), mockk())

        val client = service.newClient()
        val function = client.newFunction("test", 128)
        assertNotNull(client.findFunction(function.uid))
        function.delete()
        assertNull(client.findFunction(function.uid))

        // Delete should be idempotent
        function.delete()
    }

    @Test
    fun testClientFunctionCannotInvokeDeleted() = runSimulation {
        val service = FaaSService(dispatcher, mockk(), mockk(), mockk())

        val client = service.newClient()
        val function = client.newFunction("test", 128)
        assertNotNull(client.findFunction(function.uid))
        function.delete()

        assertThrows<IllegalStateException> { function.invoke() }
    }

    @Test
    fun testClientFunctionInvoke() = runSimulation {
        val deployer = mockk<FunctionDeployer>()
        val service = FaaSService(dispatcher, deployer, mockk(), mockk(relaxUnitFun = true))

        every { deployer.deploy(any(), any()) } answers {
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
