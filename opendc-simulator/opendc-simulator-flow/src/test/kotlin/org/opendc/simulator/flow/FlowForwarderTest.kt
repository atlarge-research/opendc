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

import io.mockk.*
import kotlinx.coroutines.*
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.simulator.flow.internal.FlowEngineImpl
import org.opendc.simulator.flow.source.FixedFlowSource

/**
 * A test suite for the [FlowForwarder] class.
 */
internal class FlowForwarderTest {
    @Test
    fun testCancelImmediately() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val forwarder = FlowForwarder(engine)
        val source = FlowSink(engine, 2000.0)

        launch { source.consume(forwarder) }

        forwarder.consume(object : FlowSource {
            override fun onPull(conn: FlowConnection, now: Long): Long {
                conn.close()
                return Long.MAX_VALUE
            }
        })

        forwarder.close()
        source.cancel()
    }

    @Test
    fun testCancel() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val forwarder = FlowForwarder(engine)
        val source = FlowSink(engine, 2000.0)

        launch { source.consume(forwarder) }

        forwarder.consume(object : FlowSource {
            var isFirst = true

            override fun onPull(conn: FlowConnection, now: Long): Long {
                return if (isFirst) {
                    isFirst = false
                    conn.push(1.0)
                    10 * 1000
                } else {
                    conn.close()
                    Long.MAX_VALUE
                }
            }
        })

        forwarder.close()
        source.cancel()
    }

    @Test
    fun testState() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val forwarder = FlowForwarder(engine)
        val consumer = object : FlowSource {
            override fun onPull(conn: FlowConnection, now: Long): Long {
                conn.close()
                return Long.MAX_VALUE
            }
        }

        assertFalse(forwarder.isActive)

        forwarder.startConsumer(consumer)
        assertTrue(forwarder.isActive)

        assertThrows<IllegalStateException> { forwarder.startConsumer(consumer) }

        forwarder.cancel()
        assertFalse(forwarder.isActive)

        forwarder.close()
        assertFalse(forwarder.isActive)
    }

    @Test
    fun testCancelPendingDelegate() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val forwarder = FlowForwarder(engine)

        val consumer = spyk(object : FlowSource {
            override fun onPull(conn: FlowConnection, now: Long): Long {
                conn.close()
                return Long.MAX_VALUE
            }
        })

        forwarder.startConsumer(consumer)
        forwarder.cancel()

        verify(exactly = 0) { consumer.onStop(any(), any()) }
    }

    @Test
    fun testCancelStartedDelegate() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val forwarder = FlowForwarder(engine)
        val source = FlowSink(engine, 2000.0)

        val consumer = spyk(FixedFlowSource(2000.0, 1.0))

        source.startConsumer(forwarder)
        yield()
        forwarder.startConsumer(consumer)
        yield()
        forwarder.cancel()

        verify(exactly = 1) { consumer.onStart(any(), any()) }
        verify(exactly = 1) { consumer.onStop(any(), any()) }
    }

    @Test
    fun testCancelPropagation() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val forwarder = FlowForwarder(engine)
        val source = FlowSink(engine, 2000.0)

        val consumer = spyk(FixedFlowSource(2000.0, 1.0))

        source.startConsumer(forwarder)
        yield()
        forwarder.startConsumer(consumer)
        yield()
        source.cancel()

        verify(exactly = 1) { consumer.onStart(any(), any()) }
        verify(exactly = 1) { consumer.onStop(any(), any()) }
    }

    @Test
    fun testExitPropagation() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val forwarder = FlowForwarder(engine, isCoupled = true)
        val source = FlowSink(engine, 2000.0)

        val consumer = object : FlowSource {
            override fun onPull(conn: FlowConnection, now: Long): Long {
                conn.close()
                return Long.MAX_VALUE
            }
        }

        source.startConsumer(forwarder)
        forwarder.consume(consumer)
        yield()

        assertFalse(forwarder.isActive)
    }

    @Test
    @Disabled // Due to Kotlin bug: https://github.com/mockk/mockk/issues/368
    fun testAdjustCapacity() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val forwarder = FlowForwarder(engine)
        val sink = FlowSink(engine, 1.0)

        val source = spyk(FixedFlowSource(2.0, 1.0))
        sink.startConsumer(forwarder)

        coroutineScope {
            launch { forwarder.consume(source) }
            delay(1000)
            sink.capacity = 0.5
        }

        assertEquals(3000, clock.millis())
        verify(exactly = 1) { source.onPull(any(), any()) }
    }

    @Test
    fun testCounters() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val forwarder = FlowForwarder(engine)
        val source = FlowSink(engine, 1.0)

        val consumer = FixedFlowSource(2.0, 1.0)
        source.startConsumer(forwarder)

        forwarder.consume(consumer)

        yield()

        assertAll(
            { assertEquals(2.0, source.counters.actual) },
            { assertEquals(source.counters.actual, forwarder.counters.actual) { "Actual work" } },
            { assertEquals(source.counters.demand, forwarder.counters.demand) { "Work demand" } },
            { assertEquals(source.counters.remaining, forwarder.counters.remaining) { "Overcommitted work" } },
            { assertEquals(2000, clock.millis()) }
        )
    }

    @Test
    fun testCoupledExit() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val forwarder = FlowForwarder(engine, isCoupled = true)
        val source = FlowSink(engine, 2000.0)

        launch { source.consume(forwarder) }

        forwarder.consume(FixedFlowSource(2000.0, 1.0))

        yield()

        assertFalse(source.isActive)
    }

    @Test
    fun testPullFailureCoupled() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val forwarder = FlowForwarder(engine, isCoupled = true)
        val source = FlowSink(engine, 2000.0)

        launch { source.consume(forwarder) }

        try {
            forwarder.consume(object : FlowSource {
                override fun onPull(conn: FlowConnection, now: Long): Long {
                    throw IllegalStateException("Test")
                }
            })
        } catch (cause: Throwable) {
            // Ignore
        }

        yield()

        assertFalse(source.isActive)
    }

    @Test
    fun testStartFailure() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val forwarder = FlowForwarder(engine)
        val source = FlowSink(engine, 2000.0)

        launch { source.consume(forwarder) }

        try {
            forwarder.consume(object : FlowSource {
                override fun onPull(conn: FlowConnection, now: Long): Long {
                    return Long.MAX_VALUE
                }

                override fun onStart(conn: FlowConnection, now: Long) {
                    throw IllegalStateException("Test")
                }
            })
        } catch (cause: Throwable) {
            // Ignore
        }

        yield()

        assertTrue(source.isActive)
        source.cancel()
    }

    @Test
    fun testConvergeFailure() = runBlockingSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val forwarder = FlowForwarder(engine)
        val source = FlowSink(engine, 2000.0)

        launch { source.consume(forwarder) }

        try {
            forwarder.consume(object : FlowSource {
                override fun onStart(conn: FlowConnection, now: Long) {
                    conn.shouldSourceConverge = true
                }

                override fun onPull(conn: FlowConnection, now: Long): Long {
                    return Long.MAX_VALUE
                }

                override fun onConverge(conn: FlowConnection, now: Long) {
                    throw IllegalStateException("Test")
                }
            })
        } catch (cause: Throwable) {
            // Ignore
        }

        yield()

        assertTrue(source.isActive)
        source.cancel()
    }
}
