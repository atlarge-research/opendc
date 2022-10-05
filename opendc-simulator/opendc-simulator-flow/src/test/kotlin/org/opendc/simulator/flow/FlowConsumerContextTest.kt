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
import org.junit.jupiter.api.*
import org.opendc.simulator.flow.internal.FlowConsumerContextImpl
import org.opendc.simulator.flow.internal.FlowEngineImpl
import org.opendc.simulator.kotlin.runSimulation

/**
 * A test suite for the [FlowConsumerContextImpl] class.
 */
class FlowConsumerContextTest {
    @Test
    fun testFlushWithoutCommand() = runSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val consumer = object : FlowSource {
            override fun onPull(conn: FlowConnection, now: Long): Long {
                return if (now == 0L) {
                    conn.push(1.0)
                    1000
                } else {
                    conn.close()
                    Long.MAX_VALUE
                }
            }
        }

        val logic = object : FlowConsumerLogic {}
        val context = FlowConsumerContextImpl(engine, consumer, logic)

        engine.scheduleSync(engine.clock.millis(), context)
    }

    @Test
    fun testDoubleStart() = runSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val consumer = object : FlowSource {
            override fun onPull(conn: FlowConnection, now: Long): Long {
                return if (now == 0L) {
                    conn.push(0.0)
                    1000
                } else {
                    conn.close()
                    Long.MAX_VALUE
                }
            }
        }

        val logic = object : FlowConsumerLogic {}
        val context = FlowConsumerContextImpl(engine, consumer, logic)

        context.start()

        assertThrows<IllegalStateException> {
            context.start()
        }
    }

    @Test
    fun testIdempotentCapacityChange() = runSimulation {
        val engine = FlowEngineImpl(coroutineContext, clock)
        val consumer = spyk(object : FlowSource {
            override fun onPull(conn: FlowConnection, now: Long): Long {
                return if (now == 0L) {
                    conn.push(1.0)
                    1000
                } else {
                    conn.close()
                    Long.MAX_VALUE
                }
            }
        })

        val logic = object : FlowConsumerLogic {}
        val context = FlowConsumerContextImpl(engine, consumer, logic)
        context.capacity = 4200.0
        context.start()
        context.capacity = 4200.0

        verify(exactly = 1) { consumer.onPull(any(), any()) }
    }
}
