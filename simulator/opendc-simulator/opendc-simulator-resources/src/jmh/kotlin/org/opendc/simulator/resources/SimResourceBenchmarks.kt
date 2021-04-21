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
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScope
import kotlinx.coroutines.test.runBlockingTest
import org.opendc.simulator.core.DelayControllerClockAdapter
import org.opendc.utils.TimerScheduler
import org.openjdk.jmh.annotations.*
import java.time.Clock
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 3, timeUnit = TimeUnit.SECONDS)
@OptIn(ExperimentalCoroutinesApi::class)
class SimResourceBenchmarks {
    private lateinit var scope: TestCoroutineScope
    private lateinit var clock: Clock
    private lateinit var scheduler: TimerScheduler<Any>

    @Setup
    fun setUp() {
        scope = TestCoroutineScope()
        clock = DelayControllerClockAdapter(scope)
        scheduler = TimerScheduler(scope.coroutineContext, clock)
    }

    @State(Scope.Thread)
    class Workload {
        lateinit var consumers: Array<SimResourceConsumer>

        @Setup
        fun setUp() {
            consumers = Array(3) { createSimpleConsumer() }
        }
    }

    @Benchmark
    fun benchmarkSource(state: Workload) {
        return scope.runBlockingTest {
            val provider = SimResourceSource(4200.0, clock, scheduler)
            return@runBlockingTest provider.consume(state.consumers[0])
        }
    }

    @Benchmark
    fun benchmarkForwardOverhead(state: Workload) {
        return scope.runBlockingTest {
            val provider = SimResourceSource(4200.0, clock, scheduler)
            val forwarder = SimResourceForwarder()
            provider.startConsumer(forwarder)
            return@runBlockingTest forwarder.consume(state.consumers[0])
        }
    }

    @Benchmark
    fun benchmarkSwitchMaxMinSingleConsumer(state: Workload) {
        return scope.runBlockingTest {
            val switch = SimResourceSwitchMaxMin(clock)

            switch.addInput(SimResourceSource(3000.0, clock, scheduler))
            switch.addInput(SimResourceSource(3000.0, clock, scheduler))

            val provider = switch.addOutput(3500.0)
            return@runBlockingTest provider.consume(state.consumers[0])
        }
    }

    @Benchmark
    fun benchmarkSwitchMaxMinTripleConsumer(state: Workload) {
        return scope.runBlockingTest {
            val switch = SimResourceSwitchMaxMin(clock)

            switch.addInput(SimResourceSource(3000.0, clock, scheduler))
            switch.addInput(SimResourceSource(3000.0, clock, scheduler))

            repeat(3) { i ->
                launch {
                    val provider = switch.addOutput(3500.0)
                    provider.consume(state.consumers[i])
                }
            }
        }
    }

    @Benchmark
    fun benchmarkSwitchExclusiveSingleConsumer(state: Workload) {
        return scope.runBlockingTest {
            val switch = SimResourceSwitchExclusive()

            switch.addInput(SimResourceSource(3000.0, clock, scheduler))
            switch.addInput(SimResourceSource(3000.0, clock, scheduler))

            val provider = switch.addOutput(3500.0)
            return@runBlockingTest provider.consume(state.consumers[0])
        }
    }

    @Benchmark
    fun benchmarkSwitchExclusiveTripleConsumer(state: Workload) {
        return scope.runBlockingTest {
            val switch = SimResourceSwitchExclusive()

            switch.addInput(SimResourceSource(3000.0, clock, scheduler))
            switch.addInput(SimResourceSource(3000.0, clock, scheduler))

            repeat(2) { i ->
                launch {
                    val provider = switch.addOutput(3500.0)
                    provider.consume(state.consumers[i])
                }
            }
        }
    }
}
