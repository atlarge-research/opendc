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

package org.opendc.experiments.base.runner

import me.tongfei.progressbar.ProgressBarBuilder
import me.tongfei.progressbar.ProgressBarStyle
import org.opendc.experiments.base.experiment.Scenario

/**
 * Run scenario when no pool is available for parallel execution
 *
 * @param experiment The scenarios to run
 */
public fun runExperiment(experiment: List<Scenario>) {
    val ansiReset = "\u001B[0m"
    val ansiGreen = "\u001B[32m"
    val ansiBlue = "\u001B[34m"

    setupOutputFolderStructure(experiment[0].outputFolder)

    val pb =
        ProgressBarBuilder().setInitialMax(experiment.sumOf { scenario -> scenario.runs.toLong() })
            .setStyle(ProgressBarStyle.ASCII)
            .setTaskName("Simulating...").build()

    for (scenario in experiment) {
        println(
            "\n\n$ansiGreen================================================================================$ansiReset",
        )
        println("$ansiBlue Running scenario: ${scenario.name} $ansiReset")
        println("$ansiGreen================================================================================$ansiReset")

        for (seed in 0..<scenario.runs) {
            println("$ansiBlue Starting seed: $seed $ansiReset")
            runScenario(scenario, seed.toLong())
            pb.step()
        }
    }
    pb.close()
}
