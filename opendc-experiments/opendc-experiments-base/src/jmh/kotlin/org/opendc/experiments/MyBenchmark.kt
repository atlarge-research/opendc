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
open class MyBenchmark : OpenDCBenchmark() {

    @Benchmark
    fun myBenchmark1() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/experiment_100.json"))
        File("output").deleteRecursively()
    }

    @Benchmark
    fun myBenchmark2() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/jmh/resources/experiments/experiment_2.json"))
        File("output").deleteRecursively()
    }
}