package org.opendc.experiments

import org.opendc.experiments.base.runner.ExperimentCommand
import org.openjdk.jmh.annotations.*
import java.util.concurrent.TimeUnit
import java.io.File

@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 1, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 3, time = 5, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
open class MyBenchmark {

    @Benchmark
    fun testFunction() {
        return myFunction()
    }

    fun myFunction() {
        ExperimentCommand().main(arrayOf("--experiment-path", "src/test/resources/experiments/experiment_1.json"))

        val someDir = File("output")
        someDir.deleteRecursively()
    }
}
