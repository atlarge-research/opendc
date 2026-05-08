/*
 * Copyright (c) 2026 AtLarge Research
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

import org.opendc.experiments.base.runner.ExperimentCommand
import org.openjdk.jmh.annotations.Benchmark
import org.openjdk.jmh.annotations.BenchmarkMode
import org.openjdk.jmh.annotations.Fork
import org.openjdk.jmh.annotations.Measurement
import org.openjdk.jmh.annotations.Mode
import org.openjdk.jmh.annotations.OutputTimeUnit
import org.openjdk.jmh.annotations.Scope
import org.openjdk.jmh.annotations.State
import org.openjdk.jmh.annotations.Warmup
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * benchmark that measures simulation performance for workloads with
 * increasing number of fragments from 1 to 10k.
 *
 * Each benchmark runs a full workload and measures runtime and [HeapStats]
 * (average and peak heap).
 *
 * Every benchmark simulates a workload consisting of 1000 tasks.
 * However, the number of fragments increases from 1 to 10k.
 *
 * The following variables are all fixed:
 * - **Topology**: 100-host cluster (`topologies/taskScaling/topology_100.json`)
 * - **Allocation policy**: memory-based (`Mem`)
 * - **Export interval**: set to 999999 to make the simulator export only at the end of the simulation,
 * to minimize the impact of telemetry export on the results.
 *
 * JFR profiling and heap statistics collection are provided by [OpenDCBenchmark].
 */
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 1, batchSize = 1)
@Measurement(iterations = 5, batchSize = 1)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class FragmentScalingBenchmark : OpenDCBenchmark() {
    /** Runs the simulation with a workload containing 1 fragment. */
    @Benchmark
    fun fragmentScalingBenchmark1() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/fragmentScaling/experiment_1.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation with a workload containing 10 fragments. */
    @Benchmark
    fun fragmentScalingBenchmark10() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/fragmentScaling/experiment_10.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation with a workload containing 100 fragments. */
    @Benchmark
    fun fragmentScalingBenchmark100() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/fragmentScaling/experiment_100.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation with a workload containing 1 000 fragments. */
    @Benchmark
    fun fragmentScalingBenchmark1K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/fragmentScaling/experiment_1K.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation with a workload containing 10 000 fragments. */
    @Benchmark
    fun fragmentScalingBenchmark10K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/fragmentScaling/experiment_10K.json"))
        File("output").deleteRecursively()
    }
}
