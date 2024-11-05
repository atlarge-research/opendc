/*
 * Copyright (c) 2024 AtLarge Research
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

@file:JvmName("M3saCliKt")

package org.opendc.experiments.m3sa.runner

import org.opendc.experiments.base.runner.runScenario
import org.opendc.experiments.base.runner.setupOutputFolderStructure
import org.opendc.experiments.base.experiment.Scenario
import java.util.concurrent.ForkJoinPool

/**
 * Run scenario when no pool is available for parallel execution
 *
 * @param experiment The scenarios to run
 * @param parallelism The number of scenarios that can be run in parallel
 */
public fun runExperiment(
    experiment: List<Scenario>,
    parallelism: Int,
) {
    val ansiReset = "\u001B[0m"
    val ansiGreen = "\u001B[32m"
    val ansiBlue = "\u001B[34m"

    setupOutputFolderStructure(experiment[0].outputFolder)

    for (scenario in experiment) {
        val pool = ForkJoinPool(parallelism)
        println(
            "\n\n$ansiGreen================================================================================$ansiReset",
        )
        println("$ansiBlue Running scenario: ${scenario.name} $ansiReset")
        println("$ansiGreen================================================================================$ansiReset")
        runScenario(
            scenario,
            pool,
        )
    }
}
