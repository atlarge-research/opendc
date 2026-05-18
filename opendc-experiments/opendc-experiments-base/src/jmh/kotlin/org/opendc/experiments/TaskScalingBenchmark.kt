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
open class TaskScalingBenchmark : OpenDCBenchmark() {

    @Benchmark
    fun taskScalingBenchmark100() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/taskScaling/experiment_100.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun taskScalingBenchmark1K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/taskScaling/experiment_1K.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun taskScalingBenchmark10K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/taskScaling/experiment_10K.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun taskScalingBenchmark100K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/taskScaling/experiment_100K.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun taskScalingBenchmark1M() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/taskScaling/experiment_1M.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun taskScalingBenchmark2_5M() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/taskScaling/experiment_2_5M.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun taskScalingBenchmark5M() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/taskScaling/experiment_5M.json"))
        File("output").deleteRecursively()
    }
}