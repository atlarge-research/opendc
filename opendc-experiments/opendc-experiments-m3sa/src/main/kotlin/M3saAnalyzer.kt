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

import kotlin.io.path.Path

/**
 * This constant variable should be changed depending on the root folder that is being run.
 * PATH_TO_PYTHON_MAIN should point to the main python file, ran when the analysis starts.
 */

public val ANALYSIS_SCRIPTS_DIRECTORY: String = "./opendc-experiments/opendc-experiments-m3sa/src/main/python"
public val ABSOLUTE_SCRIPT_PATH: String =
    Path("$ANALYSIS_SCRIPTS_DIRECTORY/main.py").toAbsolutePath().normalize().toString()
public val SCRIPT_LANGUAGE: String = "python3"

public fun m3saAnalyze(
    outputFolderPath: String,
    m3saSetupPath: String,
) {
    val process =
        ProcessBuilder(
            SCRIPT_LANGUAGE,
            ABSOLUTE_SCRIPT_PATH,
            outputFolderPath,
            m3saSetupPath,
        ).directory(Path(ANALYSIS_SCRIPTS_DIRECTORY).toFile())
            .start()

    val exitCode = process.waitFor()
    if (exitCode == 0) {
        println("[M3SA says] M3SA operation(s) completed successfully.")
    } else {
        val errors = process.errorStream.bufferedReader().readText()
        println("[M3SA says] Exit code $exitCode; Error(s): $errors")
    }
}
