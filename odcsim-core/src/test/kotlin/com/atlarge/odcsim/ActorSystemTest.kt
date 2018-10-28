/*
 * MIT License
 *
 * Copyright (c) 2018 atlarge-research
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

package com.atlarge.odcsim

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assumptions.assumeTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.lang.IllegalArgumentException

const val DELTA: Double = 0.0001

/**
 * A conformance test suite for implementors of the [ActorSystem] interface.
 */
abstract class ActorSystemTest {
    /**
     * An [ActorSystemFactory] provided by implementors to create the [ActorSystem] to be tested.
     */
    abstract val factory: ActorSystemFactory

    /**
     * Test whether the created [ActorSystem] has the correct name.
     */
    @Test
    fun `should have a name`() {
        val name = "test"
        val system = factory(object : Behavior<Unit> {}, name)

        assertEquals(name, system.name)
    }

    /**
     * Test whether creating an [ActorSystem] sets the initial time at 0.
     */
    @Test
    fun `should start at t=0`() {
        val system = factory(object : Behavior<Unit> {}, name = "test")

        assertTrue(Math.abs(system.time) < DELTA)
    }

    /**
     * Test whether an [ActorSystem] does not accept invalid points in time.
     */
    @Test
    fun `should not accept negative instants for running`() {
        val system = factory(object : Behavior<Unit> {}, name = "test")
        assertThrows<IllegalArgumentException> { system.run(-10.0) }
    }

    /**
     * Test whether an [ActorSystem] will not jump backward in time when asking to run until a specified instant
     * that has already occurred.
     */
    @Test
    fun `should not jump backward in time`() {
        val until = 10.0
        val system = factory(object : Behavior<Unit> {}, name = "test")

        system.run(until = until)
        system.run(until = until - 0.5)
        assertTrue(Math.abs(system.time - until) < DELTA)
    }

    /**
     * Test whether an [ActorSystem] will jump forward in time when asking to run until a specified instant.
     */
    @Test
    fun `should jump forward in time`() {
        val until = 10.0
        val system = factory(object : Behavior<Unit> {}, name = "test")

        assumeTrue(Math.abs(system.time) < DELTA)
        system.run(until = until)
        assertTrue(Math.abs(system.time - until) < DELTA)
    }
}
