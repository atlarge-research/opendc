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

package org.opendc.experiments.radice

import com.typesafe.config.ConfigFactory
import org.opendc.experiments.radice.scenario.ScenarioSpec
import org.opendc.experiments.radice.scenario.WorkloadSpec
import org.opendc.experiments.radice.scenario.topology.*
import org.opendc.simulator.core.runBlockingSimulation
import org.openjdk.jmh.annotations.*
import java.io.File
import java.util.concurrent.ThreadLocalRandom
import java.util.concurrent.TimeUnit

/**
 * Benchmark suite for the Radice (Risk Analysis) experiments.
 */
@State(Scope.Benchmark)
@Fork(1)
@Warmup(iterations = 4, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
class RadiceBenchmarks {
    private lateinit var runner: RadiceRunner
    private lateinit var scenario: ScenarioSpec

    /**
     * The trace to experiment with.
     */
    @Param("baseline-small")
    private lateinit var trace: String

    @Setup
    fun setUp() {
        runner = RadiceRunner(TRACE_PATH, /* no output */ null)
        scenario = ScenarioSpec.fromDefaults(
            config,
            workload = WorkloadSpec(trace),
            topology = SMALL_TOPOLOGY,
            hasInterference = false
        )
    }

    @Benchmark
    fun benchmarkRadice() = runBlockingSimulation {
        runner.runScenario(scenario, ThreadLocalRandom.current().nextLong())
    }

    companion object {
        /**
         * The configuration for the experiments.
         */
        @JvmStatic
        private val config = ConfigFactory.load().getConfig("opendc.experiments.radice")

        /**
         * The path to the traces.
         */
        @JvmStatic
        private val TRACE_PATH = File("traces")

        /**
         * The topology to use for small traces.
         */
        @JvmStatic
        private val SMALL_TOPOLOGY = TopologySpec(
            id = "small",
            clusters = listOf(
                ClusterSpec("A01", "A01", DELL_R620_FAST.copy(cpuCount = 32, memCapacity = 256.0), 1),
                ClusterSpec("B01", "B01", DELL_R710, 6),
                ClusterSpec("C01", "C01", DELL_R620_FAST, 16),
            )
        )
    }
}
