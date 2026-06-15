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

package org.opendc.experiments.m3sa
import java.nio.file.Paths

/**
 * This constant variable should be changed depending on the root folder that is being run.
 * PATH_TO_PYTHON_MAIN should point to the main python file, ran when the analysis starts.
 */

public fun m3saAnalyze(
    outputFolderPath: String,
    m3saSetupPath: String,
    m3saExecPath: String,
) {
    val isWindows: Boolean =
        System.getProperty("os.name").startsWith("Windows", ignoreCase = true)
    val command: List<String> =
        if (isWindows) {
            listOf(
                "python",
                Paths.get(m3saExecPath, "main.py")
                    .toAbsolutePath()
                    .normalize()
                    .toString(),
            )
        } else {
            listOf(
                Paths.get(m3saExecPath, "m3sa")
                    .toAbsolutePath()
                    .normalize()
                    .toString(),
            )
        }

    val process =
        ProcessBuilder(
            *command.toTypedArray(),
            m3saSetupPath,
            "$outputFolderPath/raw-output",
            "-o",
            outputFolderPath,
        )
            .redirectErrorStream(true)
            .start()

    val exitCode = process.waitFor()
    val output = process.inputStream.bufferedReader().readText()
    if (exitCode == 0) {
        println("[M3SA says] Success:\n$output")
    } else {
        println("[M3SA says] Exit code $exitCode; Output:\n$output")
        throw RuntimeException("M3SA analysis failed with exit code $exitCode")
    }
}
