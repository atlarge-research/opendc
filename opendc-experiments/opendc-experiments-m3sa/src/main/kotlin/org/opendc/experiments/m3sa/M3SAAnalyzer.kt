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

import java.nio.file.Files
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
    // script to run
    val scriptPath =
        Paths.get(m3saExecPath, "main.py")
            .toAbsolutePath()
            .normalize()
            .toString()

    // look for venv python; if missing, use system python3
    val venvPython =
        Paths.get(m3saExecPath, "venv", "bin", "python3")
            .toAbsolutePath()
            .normalize()
    val pythonBin =
        if (Files.isRegularFile(venvPython) && Files.isExecutable(venvPython)) {
            venvPython.toString()
        } else {
            "python3" // fallback
        }

    val process =
        ProcessBuilder(
            pythonBin,
            scriptPath,
            m3saSetupPath,
            "$outputFolderPath/raw-output",
            "-o",
            outputFolderPath,
        )
            .directory(Paths.get(m3saExecPath).toAbsolutePath().normalize().toFile())
            .redirectErrorStream(true)
            .start()

    val exitCode = process.waitFor()
    val output = process.inputStream.bufferedReader().readText()
    
    if (exitCode == 0) {
        println("[M3SA says] Success:\n$output")
        return
    } 

    if (output.contains("ModuleNotFoundError") || output.contains("No such file or directory")) {
        println("[M3SA] Local Python dependencies missing. Falling back to Docker (opendc-m3sa)...")
        runViaDocker(outputFolderPath, m3saSetupPath)
    } else {
        println("[M3SA says] Exit code $exitCode; Output:\n$output")
        throw RuntimeException("M3SA analysis failed with exit code $exitCode")
    }
}

private fun runViaDocker(outputFolderPath: String, m3saSetupPath: String) {
    // We mount the absolute paths to the Docker container
    val absOutput = Paths.get(outputFolderPath).toAbsolutePath().normalize().toString()
    val absSetup = Paths.get(m3saSetupPath).toAbsolutePath().normalize().toString()
    val dockerOutput = "/opt/opendc/output"
    val dockerSetup = "/opt/opendc/setup.json"

    val process = ProcessBuilder(
        "docker", "run", "--rm",
        "--entrypoint", "python3",
        // Mount output directory where M3SA writes its plots
        "-v", "$absOutput:$dockerOutput",
        // Mount the setup file directly
        "-v", "$absSetup:$dockerSetup",
        "opendc-m3sa",
        "/opt/opendc-m3sa/python/main.py",
        dockerSetup,
        "$dockerOutput/raw-output",
        "-o", dockerOutput
    )
        .redirectErrorStream(true)
        .start()

    val exitCode = process.waitFor()
    val output = process.inputStream.bufferedReader().readText()

    if (exitCode == 0) {
        println("[M3SA Docker] Success:\n$output")
    } else {
        println("[M3SA Docker] Exit code $exitCode; Output:\n$output")
        throw RuntimeException("M3SA analysis via Docker failed with exit code $exitCode. Have you built the opendc-m3sa image?")
    }
}
