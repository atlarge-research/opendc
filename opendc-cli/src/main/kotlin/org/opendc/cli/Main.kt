/*
 * Copyright (c) 2026 AtLarge Research
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

package org.opendc.cli

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.terminal.Terminal
import org.opendc.cli.config.CliConfig
import org.opendc.cli.config.toMordantTheme
import org.opendc.sdk.model.experiment.Experiment
import org.opendc.sdk.model.serialization.SdkJson

/**
 * Entry point of the `opendc` command-line interface. Loads the UI configuration once and installs a
 * terminal themed from it, so every subcommand renders with the configured colours.
 */
fun main(args: Array<String>) {
    val config = CliConfig.load()
    OpendcCommand()
        .subcommands(RunCommand(config), ValidateCommand(config), ShowCommand(config))
        .context { terminal = Terminal(theme = config.toMordantTheme()) }
        .main(args)
}

/** The root `opendc` command; it only groups the subcommands. */
internal class OpendcCommand : CliktCommand(name = "opendc") {
    override fun help(context: Context): String = "Run, validate and inspect OpenDC datacenter simulations."

    override fun run() = Unit
}

/**
 * Base for the subcommands that operate on a single experiment file. It owns the shared `experiment`
 * argument and the loader so each command declares neither, and carries the [config] they all render
 * with. A directly constructed command falls back to [CliConfig.DEFAULTS]; the real entry point
 * injects the loaded configuration.
 */
internal abstract class ExperimentCommand(
    name: String,
    protected val config: CliConfig = CliConfig.DEFAULTS,
) : CliktCommand(name) {
    protected val experimentFile by argument(name = "experiment", help = "Path to the experiment JSON file.")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)

    /** Reads and deserializes the [experimentFile] into an [Experiment], reporting a friendly error on failure. */
    protected fun loadExperiment(): Experiment =
        try {
            experimentFile.inputStream().use { SdkJson.decodeExperiment(it) }
        } catch (e: Exception) {
            throw CliktError("Could not read experiment '${experimentFile.path}': ${e.message}")
        }
}
