/*
 * Copyright (c) 2022 AtLarge Research
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

@file:JvmName("ScenarioCli")

package org.opendc.experiments.scenario

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import org.opendc.experiments.base.models.scenario.getScenario
import org.opendc.experiments.base.runner.runScenario
import java.io.File

/**
 * Main entrypoint of the application.
 */
public fun main(args: Array<String>): Unit = ScenarioCommand().main(args)

/**
 * Represents the command for the Scenario experiments.
 */
internal class ScenarioCommand : CliktCommand(name = "scenario") {
    /**
     * The path to the environment directory.
     */
    private val scenarioPath by option("--scenario-path", help = "path to scenario file")
        .file(canBeDir = false, canBeFile = true)
        .defaultLazy { File("resources/scenario.json") }

    /**
     * The number of threads to use for parallelism.
     */
    private val parallelism by option("-p", "--parallelism", help = "number of worker threads")
        .int()
        .default(Runtime.getRuntime().availableProcessors() - 1)


    override fun run() {
        val scenarios = getScenario(scenarioPath)
        runScenario(scenarios, parallelism)
        // TODO: implement outputResults(scenario) // this will take the results, from a folder, and output them visually
    }
}

/**
 * Meeting with Alexandru - tips:
 * Experiment design
 * - either combinatorial or slice and dice
 * - slice and dice = use a default option for each dimension, except for the dimension that is used at the time (there we use everything)
 * - users can specify default, or let system choose automatically the median entry
 * - placket burman design = https://en.wikipedia.org/wiki/Plackett%E2%80%93Burman_design

{
"type": "linear",
"idlePower": 350.0,
"maxPower": 350.0
}
{
"type": "linear",
"idlePower": 200.0,
"maxPower": 350.0
}
 -- these 2 models have the "exact" same output
 --> the CPUs are always at the peak level
 */
