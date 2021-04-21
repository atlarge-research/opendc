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

package org.opendc.simulator.resources

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.*
import kotlinx.coroutines.test.runBlockingTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.opendc.simulator.core.DelayControllerClockAdapter
import org.opendc.simulator.resources.consumer.SimSpeedConsumerAdapter
import org.opendc.simulator.resources.consumer.SimWorkConsumer
import org.opendc.utils.TimerScheduler

/**
 * Test suite for the [SimResourceAggregatorMaxMin] class.
 */
@OptIn(ExperimentalCoroutinesApi::class)
internal class SimResourceAggregatorMaxMinTest {
    @Test
    fun testSingleCapacity() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)

        val aggregator = SimResourceAggregatorMaxMin(clock)
        val forwarder = SimResourceForwarder()
        val sources = listOf(
            forwarder,
            SimResourceSource(1.0, clock, scheduler)
        )
        sources.forEach(aggregator::addInput)

        val consumer = SimWorkConsumer(1.0, 0.5)
        val usage = mutableListOf<Double>()
        val source = SimResourceSource(1.0, clock, scheduler)
        val adapter = SimSpeedConsumerAdapter(forwarder, usage::add)
        source.startConsumer(adapter)

        try {
            aggregator.output.consume(consumer)
            yield()

            assertAll(
                { assertEquals(1000, currentTime) },
                { assertEquals(listOf(0.0, 0.5, 0.0), usage) }
            )
        } finally {
            aggregator.output.close()
        }
    }

    @Test
    fun testDoubleCapacity() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)

        val aggregator = SimResourceAggregatorMaxMin(clock)
        val sources = listOf(
            SimResourceSource(1.0, clock, scheduler),
            SimResourceSource(1.0, clock, scheduler)
        )
        sources.forEach(aggregator::addInput)

        val consumer = SimWorkConsumer(2.0, 1.0)
        val usage = mutableListOf<Double>()
        val adapter = SimSpeedConsumerAdapter(consumer, usage::add)

        try {
            aggregator.output.consume(adapter)
            yield()
            assertAll(
                { assertEquals(1000, currentTime) },
                { assertEquals(listOf(0.0, 2.0, 0.0), usage) }
            )
        } finally {
            aggregator.output.close()
        }
    }

    @Test
    fun testOvercommit() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)

        val aggregator = SimResourceAggregatorMaxMin(clock)
        val sources = listOf(
            SimResourceSource(1.0, clock, scheduler),
            SimResourceSource(1.0, clock, scheduler)
        )
        sources.forEach(aggregator::addInput)

        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any()) }
            .returns(SimResourceCommand.Consume(4.0, 4.0, 1000))
            .andThen(SimResourceCommand.Exit)

        try {
            aggregator.output.consume(consumer)
            yield()
            assertEquals(1000, currentTime)

            verify(exactly = 2) { consumer.onNext(any()) }
        } finally {
            aggregator.output.close()
        }
    }

    @Test
    fun testException() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)

        val aggregator = SimResourceAggregatorMaxMin(clock)
        val sources = listOf(
            SimResourceSource(1.0, clock, scheduler),
            SimResourceSource(1.0, clock, scheduler)
        )
        sources.forEach(aggregator::addInput)

        val consumer = mockk<SimResourceConsumer>(relaxUnitFun = true)
        every { consumer.onNext(any()) }
            .returns(SimResourceCommand.Consume(1.0, 1.0))
            .andThenThrows(IllegalStateException())

        try {
            assertThrows<IllegalStateException> { aggregator.output.consume(consumer) }
            yield()
            assertEquals(SimResourceState.Pending, sources[0].state)
        } finally {
            aggregator.output.close()
        }
    }

    @Test
    fun testAdjustCapacity() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)

        val aggregator = SimResourceAggregatorMaxMin(clock)
        val sources = listOf(
            SimResourceSource(1.0, clock, scheduler),
            SimResourceSource(1.0, clock, scheduler)
        )
        sources.forEach(aggregator::addInput)

        val consumer = SimWorkConsumer(4.0, 1.0)
        try {
            coroutineScope {
                launch { aggregator.output.consume(consumer) }
                delay(1000)
                sources[0].capacity = 0.5
            }
            yield()
            assertEquals(2334, currentTime)
        } finally {
            aggregator.output.close()
        }
    }

    @Test
    fun testFailOverCapacity() = runBlockingTest {
        val clock = DelayControllerClockAdapter(this)
        val scheduler = TimerScheduler<Any>(coroutineContext, clock)

        val aggregator = SimResourceAggregatorMaxMin(clock)
        val sources = listOf(
            SimResourceSource(1.0, clock, scheduler),
            SimResourceSource(1.0, clock, scheduler)
        )
        sources.forEach(aggregator::addInput)

        val consumer = SimWorkConsumer(1.0, 0.5)
        try {
            coroutineScope {
                launch { aggregator.output.consume(consumer) }
                delay(500)
                sources[0].capacity = 0.5
            }
            yield()
            assertEquals(1000, currentTime)
        } finally {
            aggregator.output.close()
        }
    }
}
