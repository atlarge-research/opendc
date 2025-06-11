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

@file:JvmName("M3SACli")

package org.opendc.experiments.m3sa.runner

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.default
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.int
import m3saAnalyze
import org.opendc.experiments.base.experiment.getExperiment
import org.opendc.experiments.m3sa.scenario.getOutputFolder
import java.io.File

/**
 * Main entrypoint of the application.
 */
public fun main(args: Array<String>): Unit = M3SACommand().main(args)

/**
 * Represents the command for the Scenario experiments.
 */
internal class M3SACommand : CliktCommand(name = "experiment") {
    /**
     * The path to the environment directory.
     */
    private val scenarioPath by option("--experiment-path", help = "path to experiment file")
        .file(canBeDir = false, canBeFile = true)
        .defaultLazy { File("resources/experiment.json") }

    private val m3saPath by option("-m", "--m3sa-setup-path", help = "path to m3sa setup file")
        .file(canBeDir = false, canBeFile = true)

    private val m3saExec by option("-e", "--m3sa-exec-path", help = "path to m3sa executable")
        .file(canBeDir = true, canBeFile = false)
        .defaultLazy { File("opendc-experiments/opendc-experiments-m3sa/src/main/python/") }

    private val inputIterations by option("-i", "--iterations", help = "number of iterations to run")
        .int()
        .default(1)

    override fun run() {
        val file = File("analysis.txt")
        if (!file.exists()) {
            file.createNewFile()
        }

        var iterations = inputIterations
        var currentIteration = 1

        while (iterations > 0) {
            val startTime = System.currentTimeMillis()
            val experiment = getExperiment(scenarioPath)
            org.opendc.experiments.base.runner.runExperiment(experiment)
            val simulationEnd = System.currentTimeMillis()
            println("Simulation time: ${(simulationEnd - startTime) / 1000} ms")

            if (m3saPath != null) {
                m3saAnalyze(
                    outputFolderPath = getOutputFolder(scenarioPath),
                    m3saSetupPath = m3saPath.toString(),
                    m3saExecPath = m3saExec.toString(),
                )
            } else {
                println(
                    "\n" +
                        "===================================================\n" +
                        "|M3SA path is not provided. Skipping M3SA analysis.|\n" +
                        "===================================================",
                )
            }

            val endTime = System.currentTimeMillis()
            println("OpenDC time: ${(simulationEnd - startTime) / 1000.0} s")
            println("M3SA time: ${(endTime - simulationEnd) / 1000.0} s")
            println("Total operation time: ${(endTime - startTime) / 1000.0} s")

            file.appendText("${currentIteration}. OpenDC time: ${(simulationEnd - startTime) / 1000.0} s\n")
            file.appendText("${currentIteration}. M3SA time: ${(endTime - simulationEnd) / 1000.0} s\n")
            file.appendText("${currentIteration}. Total operation time: ${(endTime - startTime) / 1000.0} s\n\n")

            iterations -= 1
            currentIteration += 1
        }

        file.appendText("===================================================\n")
        println("Finished $scenarioPath")
    }
}
