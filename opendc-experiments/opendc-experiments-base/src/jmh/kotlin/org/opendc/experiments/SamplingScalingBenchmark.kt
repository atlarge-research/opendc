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
open class SamplingScalingBenchmark : OpenDCBenchmark() {

    @Benchmark
    fun samplingScalingBenchmark1() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_1.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun samplingScalingBenchmark10() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_10.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun samplingScalingBenchmark100() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_100.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun samplingScalingBenchmark1K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_1K.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun samplingScalingBenchmark5K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_5K.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun samplingScalingBenchmark10K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_10K.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun samplingScalingBenchmark20K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_20K.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun samplingScalingBenchmark30K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_30K.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun samplingScalingBenchmark40K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_40K.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun samplingScalingBenchmark50K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_50K.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun samplingScalingBenchmark60K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_60K.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun samplingScalingBenchmark70K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_70K.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun samplingScalingBenchmark80K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_80K.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun samplingScalingBenchmark90K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_90K.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun samplingScalingBenchmark100K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_100K.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun samplingScalingBenchmark200K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_200K.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun samplingScalingBenchmark360K() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/samplingScaling/experiment_360K.json"))
        File("output").deleteRecursively()
    }
}