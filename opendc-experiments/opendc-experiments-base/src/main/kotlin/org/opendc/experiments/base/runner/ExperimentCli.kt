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

@file:JvmName("ExperimentCli")

package org.opendc.experiments.base.runner

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.options.defaultLazy
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import org.opendc.experiments.base.experiment.getExperiment
import java.io.File

/**
 * Main entrypoint of the application.
 */
public fun main(args: Array<String>): Unit = ExperimentCommand().main(args)

/**
 * Represents the command for the Scenario experiments.
 */
internal class ExperimentCommand : CliktCommand(name = "experiment") {
    /**
     * The path to the environment directory.
     */
    private val scenarioPath by option("--experiment-path", help = "path to experiment file")
        .file(canBeDir = false, canBeFile = true)
        .defaultLazy { File("resources/experiment.json") }

    override fun run() {
        val experiment = getExperiment(scenarioPath)
        runExperiment(experiment)
    }
}
