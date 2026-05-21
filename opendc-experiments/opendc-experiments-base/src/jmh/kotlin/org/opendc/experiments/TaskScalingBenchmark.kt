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
 * JMH benchmark that measures simulation performance as the number of scheduled
 * tasks scales from 100 to 5 000 000. All tasks have a single fragment.
 *
 * Each benchmark method runs a full simulation with an increasing task count while
 * keeping all other variables fixed:
 * - **Topology**: 100-host cluster (`topologies/taskScaling/topology_100.json`)
 * - **Allocation policy**: memory-based (`Mem`)
 * - **Export interval**: set to 999999 to make the simulator export only at the end of the simulation,
 *
 * JFR profiling and heap statistics collection are provided by [OpenDCBenchmark].
 */
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 1, batchSize = 1)
@Measurement(iterations = 5, batchSize = 1)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class TaskScalingBenchmark : OpenDCBenchmark() {
    /** Runs the simulation with a workload of 100 tasks. */
    @Benchmark
    fun taskScalingBenchmark100() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/taskScaling/experiment_100.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation with a workload of 1 000 tasks. */
    @Benchmark
    fun taskScalingBenchmark1K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/taskScaling/experiment_1K.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation with a workload of 10 000 tasks. */
    @Benchmark
    fun taskScalingBenchmark10K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/taskScaling/experiment_10K.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation with a workload of 100 000 tasks. */
    @Benchmark
    fun taskScalingBenchmark100K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/taskScaling/experiment_100K.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation with a workload of 1 000 000 tasks. */
    @Benchmark
    fun taskScalingBenchmark1M() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/taskScaling/experiment_1M.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation with a workload of 2 500 000 tasks. */
    @Benchmark
    fun taskScalingBenchmark2M500K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/taskScaling/experiment_2_5M.json"))
        File("output").deleteRecursively()
    }
}
