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
import com.github.ajalt.clikt.core.obj
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.mordant.terminal.Terminal
import org.opendc.cli.config.CliConfig
import org.opendc.cli.config.toMordantTheme
import org.opendc.cli.legacy.readLegacyExperiment
import org.opendc.sdk.model.experiment.ExperimentSpec
import java.nio.file.Path

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

/**
 * The root `opendc` command. It groups the subcommands and settles how their experiment file is to be
 * read, publishing that choice on the context every subcommand inherits.
 */
internal class OpendcCommand : CliktCommand(name = "opendc") {
    private val legacy by option(
        "--legacy",
        help = "Read experiment files written in the deprecated opendc-experiments JSON format.",
    ).flag()

    private val strict by option(
        "--strict",
        help = "Reject experiment files that contain unknown keys instead of ignoring them.",
    ).flag()

    override fun help(context: Context): String = "Run, validate and inspect OpenDC datacenter simulations."

    override fun run() {
        currentContext.obj = ExperimentReadOptions(legacy = legacy, strict = strict)
    }
}

/**
 * How the root command was told to read each subcommand's experiment file: in which format ([legacy])
 * and how tolerantly ([strict]). Published on the context so every subcommand reads the same choice.
 */
internal data class ExperimentReadOptions(
    val legacy: Boolean,
    val strict: Boolean,
)

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

    /** Whether the root command was asked to read the experiment in the deprecated format. */
    protected val isLegacy: Boolean
        get() = currentContext.findObject<ExperimentReadOptions>()?.legacy == true

    /** Whether the root command was asked to reject experiments that carry unknown keys. */
    protected val isStrict: Boolean
        get() = currentContext.findObject<ExperimentReadOptions>()?.strict == true

    /**
     * The directory the experiment's relative paths resolve against, absent an explicit `--input-root`.
     *
     * A legacy experiment names its topologies and traces relative to the directory it is *run from*,
     * because that is what the deprecated runner resolved against; an SDK experiment names its traces
     * relative to the file itself. The topologies inlined by [loadExperiment] and the traces the runner
     * provisions later must share this one root, or the two halves of the same experiment would be
     * looked up in different places.
     */
    protected val defaultInputRoot: Path
        get() =
            if (isLegacy) {
                Path.of("").toAbsolutePath()
            } else {
                experimentFile.absoluteFile.parentFile.toPath()
            }

    /**
     * Reads and deserializes the [experimentFile] into an [ExperimentSpec], reporting a friendly error on
     * failure. Under the root command's `--legacy` flag the file is read as a deprecated
     * `opendc-experiments-base` document and converted, resolving the topologies it references against
     * [root]; otherwise it is SDK-model JSON, which composes the files it names with `importFrom`
     * relative to itself and so needs no root.
     */
    protected fun loadExperiment(root: Path = defaultInputRoot): ExperimentSpec =
        try {
            if (isLegacy) {
                readLegacyExperiment(experimentFile, root.toFile(), isStrict)
            } else {
                readExperiment(experimentFile, isStrict)
            }
        } catch (e: Exception) {
            throw CliktError("Could not read experiment '${experimentFile.path}': ${e.message}")
        }
}
