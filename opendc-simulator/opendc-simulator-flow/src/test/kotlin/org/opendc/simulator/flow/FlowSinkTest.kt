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

package org.opendc.simulator.flow

import io.mockk.spyk
import io.mockk.verify
import kotlinx.coroutines.*
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.opendc.simulator.flow.internal.FlowEngineImpl
import org.opendc.simulator.flow.source.FixedFlowSource
import org.opendc.simulator.flow.source.FlowSourceRateAdapter
import org.opendc.simulator.kotlin.runBlockingSimulation

/**
 * A test suite for the [FlowSink] class.
 */
internal class FlowSinkTest {
    @Test
    fun testSpeed() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val capacity = 4200.0
        val provider = FlowSink(engine, capacity)

        val consumer = FixedFlowSource(4200.0, 1.0)

        val res = mutableListOf<Double>()
        val adapter = FlowSourceRateAdapter(consumer, res::add)

        provider.consume(adapter)

        assertEquals(listOf(0.0, capacity, 0.0), res) { "Speed is reported correctly" }
    }

    @Test
    fun testAdjustCapacity() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val provider = FlowSink(engine, 1.0)

        val consumer = spyk(FixedFlowSource(2.0, 1.0))

        coroutineScope {
            launch { provider.consume(consumer) }
            delay(1000)
            provider.capacity = 0.5
        }
        assertEquals(3000, clock.millis())
        verify(exactly = 3) { consumer.onPull(any(), any()) }
    }

    @Test
    fun testSpeedLimit() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val capacity = 4200.0
        val provider = FlowSink(engine, capacity)

        val consumer = FixedFlowSource(capacity, 2.0)

        val res = mutableListOf<Double>()
        val adapter = FlowSourceRateAdapter(consumer, res::add)

        provider.consume(adapter)

        assertEquals(listOf(0.0, capacity, 0.0), res) { "Speed is reported correctly" }
    }

    /**
     * Test to see whether no infinite recursion occurs when interrupting during [FlowSource.onStart] or
     * [FlowSource.onPull].
     */
    @Test
    fun testIntermediateInterrupt() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val capacity = 4200.0
        val provider = FlowSink(engine, capacity)

        val consumer = object : FlowSource {
            override fun onPull(conn: FlowConnection, now: Long): Long {
                conn.close()
                return Long.MAX_VALUE
            }

            override fun onStart(conn: FlowConnection, now: Long) {
                conn.pull()
            }
        }

        provider.consume(consumer)
    }

    @Test
    fun testInterrupt() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val capacity = 4200.0
        val provider = FlowSink(engine, capacity)
        lateinit var resCtx: FlowConnection

        val consumer = object : FlowSource {
            var isFirst = true

            override fun onStart(conn: FlowConnection, now: Long) {
                resCtx = conn
            }

            override fun onPull(conn: FlowConnection, now: Long): Long {
                return if (isFirst) {
                    isFirst = false
                    conn.push(1.0)
                    4000
                } else {
                    conn.close()
                    Long.MAX_VALUE
                }
            }
        }

        launch {
            yield()
            resCtx.pull()
        }
        provider.consume(consumer)

        assertEquals(0, clock.millis())
    }

    @Test
    fun testFailure() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val capacity = 4200.0
        val provider = FlowSink(engine, capacity)

        val consumer = object : FlowSource {
            override fun onStart(conn: FlowConnection, now: Long) {
                throw IllegalStateException("Hi")
            }

            override fun onPull(conn: FlowConnection, now: Long): Long {
                return Long.MAX_VALUE
            }
        }

        assertThrows<IllegalStateException> {
            provider.consume(consumer)
        }
    }

    @Test
    fun testExceptionPropagationOnNext() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val capacity = 4200.0
        val provider = FlowSink(engine, capacity)

        val consumer = object : FlowSource {
            var isFirst = true

            override fun onPull(conn: FlowConnection, now: Long): Long {
                return if (isFirst) {
                    isFirst = false
                    conn.push(1.0)
                    1000
                } else {
                    throw IllegalStateException()
                }
            }
        }

        assertThrows<IllegalStateException> {
            provider.consume(consumer)
        }
    }

    @Test
    fun testConcurrentConsumption() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val capacity = 4200.0
        val provider = FlowSink(engine, capacity)

        val consumer = FixedFlowSource(capacity, 1.0)

        assertThrows<IllegalStateException> {
            coroutineScope {
                launch { provider.consume(consumer) }
                provider.consume(consumer)
            }
        }
    }

    @Test
    fun testCancelDuringConsumption() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val capacity = 4200.0
        val provider = FlowSink(engine, capacity)

        val consumer = FixedFlowSource(capacity, 1.0)

        launch { provider.consume(consumer) }
        delay(500)
        provider.cancel()

        yield()

        assertEquals(500, clock.millis())
    }

    @Test
    fun testInfiniteSleep() {
        assertThrows<IllegalStateException> {
            runBlockingSimulation {
                val engine = FlowEngineImpl(coroutineContext, clock)
                val capacity = 4200.0
                val provider = FlowSink(engine, capacity)

                val consumer = object : FlowSource {
                    override fun onPull(conn: FlowConnection, now: Long): Long = Long.MAX_VALUE
                }

                provider.consume(consumer)
            }
        }
    }
}
