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
import java.lang.ProcessBuilder

/**
 * This constant variable should be changed depending on the root folder that is being run.
 * PATH_TO_PYTHON_MAIN should point to the main python file, ran when the analysis starts.
 */

public const val PATH_TO_PYTHON_FOLDER: String = "../opendc-analyze/src/main/python"
public const val PATH_TO_PYHON_MAIN_SCRIPT: String = "$PATH_TO_PYTHON_FOLDER/main.py"


public fun analyzeResults(outputFolderPath: String) {
    val pathh = Path(PATH_TO_PYHON_MAIN_SCRIPT).toAbsolutePath().normalize().toString()
    val process = ProcessBuilder(
        "python3",
        Path(PATH_TO_PYHON_MAIN_SCRIPT).toAbsolutePath().normalize().toString(),
        outputFolderPath
    )
        .directory(Path(PATH_TO_PYTHON_FOLDER).toFile())
        .start()

//    print("The absolute path is ", Path(PATH_TO_PYTHON_FOLDER).toAbsolutePath().normalize().toString())

    val exitCode = process.waitFor()
    if (exitCode != 0) {
        val errors = process.errorStream.bufferedReader().readText()
        println("Errors: $errors")
    }
}
