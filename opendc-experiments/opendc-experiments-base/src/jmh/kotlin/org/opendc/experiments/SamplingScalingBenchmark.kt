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
 * JMH benchmark that measures the cost of telemetry export as the number of
 * export samples scales from 1 to 360 000.
 *
 * The number suffix in each benchmark method name is the number of export samples
 * produced during the simulation. This is controlled via the `exportInterval` in
 * the experiment JSON: a smaller interval means more frequent sampling and therefore
 * more data written. The workload and topology are fixed across all tiers so that
 * only the export overhead changes:
 * - **Topology**: 100-host cluster (`topologies/taskScaling/topology_100.json`)
 * - **Workload**: 10 000-task trace (`workloadTraces/taskScaling/10K_tasks`)
 * - **Allocation policy**: memory-based (`Mem`)
 *
 * JFR profiling and heap statistics collection are provided by [OpenDCBenchmark].
 */
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 1, batchSize = 1)
@Measurement(iterations = 5, batchSize = 1)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class SamplingScalingBenchmark : OpenDCBenchmark() {
    /** Runs the simulation producing 1 export sample. */
    @Benchmark
    fun samplingScalingBenchmark1() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_1.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation producing 10 export samples. */
    @Benchmark
    fun samplingScalingBenchmark10() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_10.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation producing 100 export samples. */
    @Benchmark
    fun samplingScalingBenchmark100() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_100.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation producing 1 000 export samples. */
    @Benchmark
    fun samplingScalingBenchmark1K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_1K.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation producing 5 000 export samples. */
    @Benchmark
    fun samplingScalingBenchmark5K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_5K.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation producing 10 000 export samples. */
    @Benchmark
    fun samplingScalingBenchmark10K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_10K.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation producing 20 000 export samples. */
    @Benchmark
    fun samplingScalingBenchmark20K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_20K.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation producing 30 000 export samples. */
    @Benchmark
    fun samplingScalingBenchmark30K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_30K.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation producing 40 000 export samples. */
    @Benchmark
    fun samplingScalingBenchmark40K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_40K.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation producing 50 000 export samples. */
    @Benchmark
    fun samplingScalingBenchmark50K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_50K.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation producing 60 000 export samples. */
    @Benchmark
    fun samplingScalingBenchmark60K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_60K.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation producing 70 000 export samples. */
    @Benchmark
    fun samplingScalingBenchmark70K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_70K.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation producing 80 000 export samples. */
    @Benchmark
    fun samplingScalingBenchmark80K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_80K.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation producing 90 000 export samples. */
    @Benchmark
    fun samplingScalingBenchmark90K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_90K.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation producing 100 000 export samples. */
    @Benchmark
    fun samplingScalingBenchmark100K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_100K.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation producing 200 000 export samples. */
    @Benchmark
    fun samplingScalingBenchmark200K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_200K.json"))
        File("output").deleteRecursively()
    }

    /** Runs the simulation producing 360 000 export samples. */
    @Benchmark
    fun samplingScalingBenchmark360K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_360K.json"))
        File("output").deleteRecursively()
    }
}
