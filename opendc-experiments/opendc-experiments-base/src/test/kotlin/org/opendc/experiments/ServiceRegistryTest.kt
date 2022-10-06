/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.experiments

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.experiments.internal.ServiceRegistryImpl

/**
 * Test suite for the [ServiceRegistry] implementation.
 */
class ServiceRegistryTest {
    @Test
    fun testRetrievalSuccess() {
        val registry = ServiceRegistryImpl()

        registry.register("opendc.org", String::class.java, "Comment")

        assertEquals("Comment", registry.resolve("opendc.org", String::class.java))
    }

    @Test
    fun testRetrievalFailure() {
        val registry = ServiceRegistryImpl()
        assertNull(registry.resolve("opendc.org", String::class.java))
    }

    @Test
    fun testDuplicate() {
        val registry = ServiceRegistryImpl()

        registry.register("opendc.org", String::class.java, "Comment")

        assertThrows<IllegalStateException> { registry.register("opendc.org", String::class.java, "Comment2") }
    }

    @Test
    fun testRemove() {
        val registry = ServiceRegistryImpl()

        registry.register("opendc.org", String::class.java, "Comment")
        registry.remove("opendc.org", String::class.java)

        assertAll(
            { assertDoesNotThrow { registry.remove("opendc.org", String::class.java) } },
            { assertNull(registry.resolve("opendc.org", String::class.java)) }
        )
    }

    @Test
    fun testRemoveNonExistent() {
        val registry = ServiceRegistryImpl()

        assertAll(
            { assertNull(registry.resolve("opendc.org", String::class.java)) },
            { assertDoesNotThrow { registry.remove("opendc.org", String::class.java) } }
        )
    }

    @Test
    fun testRemoveAll() {
        val registry = ServiceRegistryImpl()

        registry.register("opendc.org", String::class.java, "Comment")
        registry.register("opendc.org", Int::class.java, 1)

        println(registry)

        registry.remove("opendc.org")

        assertAll(
            { assertNull(registry.resolve("opendc.org", String::class.java)) },
            { assertNull(registry.resolve("opendc.org", Int::class.java)) }
        )
    }

    @Test
    fun testClone() {
        val registry = ServiceRegistryImpl()
        registry.register("opendc.org", String::class.java, "Comment")

        val clone = registry.clone()
        clone.remove("opendc.org")

        assertAll(
            { assertEquals("Comment", registry.resolve("opendc.org", String::class.java)) },
            { assertNull(clone.resolve("opendc.org", String::class.java)) }
        )
    }
}
