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

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.opendc.simulator.resources.consumer.SimTraceConsumer
import org.opendc.simulator.utils.DelayControllerClockAdapter
import org.opendc.utils.TimerScheduler
import org.openjdk.jmh.annotations.*
import java.time.Clock
import java.util.concurrent.TimeUnit

@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 0)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@OptIn(ExperimentalCoroutinesApi::class)
class SimResourceSwitchBenchmark {
    private lateinit var scope: TestCoroutineScope
    private lateinit var clock: Clock
    private lateinit var scheduler: TimerScheduler<Any>
    private lateinit var consumer: SimResourceConsumer<SimGenericResource>

    @Setup
    fun setUp() {
        scope = TestCoroutineScope()
        clock = DelayControllerClockAdapter(scope)
        scheduler = TimerScheduler(scope.coroutineContext, clock)
        consumer =
            SimTraceConsumer(
                sequenceOf(
                    SimTraceConsumer.Fragment(1000, 28.0),
                    SimTraceConsumer.Fragment(1000, 3500.0),
                    SimTraceConsumer.Fragment(1000, 0.0),
                    SimTraceConsumer.Fragment(1000, 183.0)
                ),
            )
    }

    @Benchmark
    fun benchmarkSwitch() {
        return scope.runBlockingTest {
            val switch = SimResourceSwitchMaxMin<SimGenericResource>(clock)

            switch.addInput(SimResourceSource(SimGenericResource(3000.0), clock, scheduler))
            switch.addInput(SimResourceSource(SimGenericResource(3000.0), clock, scheduler))

            val provider = switch.addOutput(SimGenericResource(3500.0))
            return@runBlockingTest provider.consume(consumer)
        }
    }

    data class SimGenericResource(override val capacity: Double) : SimResource
}
