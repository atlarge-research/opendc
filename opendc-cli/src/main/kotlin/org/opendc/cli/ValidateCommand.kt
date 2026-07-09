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
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.ProgramResult
import com.github.ajalt.clikt.core.terminal
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.types.file

/** `opendc validate` — parse an experiment file and report any configuration issues. */
internal class ValidateCommand : CliktCommand(name = "validate") {
    override fun help(context: Context): String = "Validate an experiment file and report any configuration issues."

    private val experimentFile by argument(name = "experiment", help = "Path to the experiment JSON file.")
        .file(mustExist = true, canBeDir = false, mustBeReadable = true)

    override fun run() {
        val experiment = loadExperiment(experimentFile)
        val issues = experiment.validate()

        if (issues.isEmpty()) {
            terminal.println(terminal.theme.success("✓ ${experimentFile.name} is valid"))
            return
        }

        terminal.println(terminal.theme.danger("✗ ${experimentFile.name} has ${issues.size} issue(s):"))
        issues.forEach { terminal.println("  • ${it.path}: ${it.message}") }
        throw ProgramResult(1)
    }
}
