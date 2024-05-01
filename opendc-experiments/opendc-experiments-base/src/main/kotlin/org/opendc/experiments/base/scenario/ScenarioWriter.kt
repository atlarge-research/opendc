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

package org.opendc.experiments.base.models.scenario

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.opendc.experiments.base.scenario.specs.ScenarioSpec
import java.io.File

/**
 * A writer for writing scenarios to a file.
 * @param jsonText The JSON text to write to the file, which is constantly updated during the writing process.
 * @param json The JSON object used to encode the scenario specification.
 */
public class ScenarioWriter {
    private var jsonText = "["
    private val json = Json { prettyPrint = true }

    /**
     * Write the given [scenarioSpec] to the given [file].
     */
    public fun write(
        scenarioSpec: ScenarioSpec,
        file: File,
    ) {
        openArray(file)
        val jsonString = json.encodeToString(scenarioSpec) + ","
        jsonText += jsonString + "\n"
        file.writeText(jsonText)
        closeArray(file)
    }

    /**
     * Delete the last character of the file.
     */
    private fun openArray(file: File) {
        val text = file.readText()
        file.writeText(text.dropLast(0))
    }

    /**
     * Add the closing bracket to the file.
     */
    private fun closeArray(file: File) {
        file.appendText("]")
    }
}
