package org.opendc.experiments

import org.opendc.experiments.base.runner.ExperimentCommand
import org.openjdk.jmh.annotations.*
import java.io.File
import java.util.concurrent.TimeUnit

@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 1, batchSize = 1)
@Measurement(iterations = 5, batchSize = 1)
@BenchmarkMode(Mode.SingleShotTime)
@OutputTimeUnit(TimeUnit.MILLISECONDS)
open class FragmentScalingBenchmark : OpenDCBenchmark() {

    @Benchmark
    fun fragmentScalingBenchmark1() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/fragmentScaling/experiment_1.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun fragmentScalingBenchmark10() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/fragmentScaling/experiment_10.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun fragmentScalingBenchmark100() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/fragmentScaling/experiment_100.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun fragmentScalingBenchmark1K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/fragmentScaling/experiment_1K.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun fragmentScalingBenchmark10K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/fragmentScaling/experiment_10K.json"))
        File("output").deleteRecursively()
    }
}