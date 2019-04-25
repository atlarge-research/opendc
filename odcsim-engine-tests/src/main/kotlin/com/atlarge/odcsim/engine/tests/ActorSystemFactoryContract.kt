/*
 * MIT License
 *
 * Copyright (c) 2019 atlarge-research
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

package com.atlarge.odcsim.engine.tests

import com.atlarge.odcsim.ActorSystemFactory
import com.atlarge.odcsim.empty
import com.atlarge.odcsim.setup
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

/**
 * A conformance test suite for implementors of the [ActorSystemFactory] interface.
 */
abstract class ActorSystemFactoryContract {
    /**
     * Create an [ActorSystemFactory] instance to test.
     */
    abstract fun createFactory(): ActorSystemFactory

    /**
     * Test whether the factory will create an [ActorSystem] with correct name.
     */
    @Test
    fun `should create a system with correct name`() {
        val factory = createFactory()
        val name = "test"
        val system = factory(empty<Unit>(), name)

        assertEquals(name, system.name)
    }

    /**
     * Test whether the factory will create an [ActorSystem] with valid root behavior.
     */
    @Test
    fun `should create a system with correct root behavior`() {
        val factory = createFactory()
        val system = factory(setup<Unit> { throw UnsupportedOperationException() }, "test")

        assertThrows<UnsupportedOperationException> { system.run(until = 10.0) }
    }
}
