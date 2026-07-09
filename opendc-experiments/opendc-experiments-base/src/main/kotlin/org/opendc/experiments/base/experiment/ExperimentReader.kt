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

@file:Suppress("DEPRECATION")

package org.opendc.experiments.base.experiment

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.opendc.common.logger.logger
import org.opendc.compute.simulator.telemetry.parquet.ComputeExportConfig
import org.opendc.experiments.base.experiment.specs.ExperimentSpec
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.inputStream

public class ExperimentReader {
    private val jsonReader = Json { ignoreUnknownKeys = true }
    private val strictJsonReader = Json { ignoreUnknownKeys = false }

    public fun read(
        file: File,
        strictReader: Boolean = false,
    ): ExperimentSpec = read(file.inputStream(), strictReader)

    public fun read(
        path: Path,
        strictReader: Boolean = false,
    ): ExperimentSpec = read(path.inputStream(), strictReader)

    /**
     * Read the specified [input].
     */
    public fun read(
        input: InputStream,
        strictReader: Boolean = false,
    ): ExperimentSpec {
        // Loads the default parquet output fields,
        // so that they can be deserialized
        ComputeExportConfig.loadDfltColumns()

        val text = input.bufferedReader().use { it.readText() }

        if (strictReader) {
            val experiment = strictJsonReader.decodeFromString<ExperimentSpec>(text)
            experiment.validate()
            return experiment
        }

        val experiment = jsonReader.decodeFromString<ExperimentSpec>(text)

        // [jsonReader] ignores unknown keys, so typos and stale fields would otherwise pass silently.
        // Decode once more with a strict reader to surface them: the two readers differ only in
        // [JsonBuilder.ignoreUnknownKeys], and the lenient decode above already succeeded, so any
        // failure here can only be caused by an unknown key that is being ignored.
        try {
            strictJsonReader.decodeFromString<ExperimentSpec>(text)
        } catch (e: SerializationException) {
            LOG.warn("The experiment file contains an unknown key that is ignored: ${e.message?.substringBefore('\n')}")
        }

        experiment.validate()

        return experiment
    }

    private companion object {
        private val LOG by logger("org.opendc.experiments.base.experiment.ExperimentReader")
    }
}
