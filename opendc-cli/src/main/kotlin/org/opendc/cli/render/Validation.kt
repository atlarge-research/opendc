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

package org.opendc.cli.render

import com.github.ajalt.mordant.terminal.Terminal
import org.opendc.cli.config.CliConfig
import org.opendc.sdk.model.validation.ValidationIssue

/**
 * Prints the outcome of validating an experiment named [name] and returns whether it is valid. Shared
 * by `opendc validate` (which shows the success line and exits non-zero on failure) and `opendc run`
 * (which only needs the failure path, so it passes [showSuccess] = false).
 */
internal fun renderValidation(
    terminal: Terminal,
    name: String,
    issues: List<ValidationIssue>,
    config: CliConfig,
    showSuccess: Boolean = true,
): Boolean {
    if (issues.isEmpty()) {
        if (showSuccess) terminal.println(terminal.theme.success("${config.symbols.success} $name is valid"))
        return true
    }
    terminal.println(terminal.theme.danger("${config.symbols.failure} $name has ${issues.size} issue(s):"))
    issues.forEach { terminal.println("  ${config.symbols.bullet} ${it.path}: ${it.message}") }
    return false
}
