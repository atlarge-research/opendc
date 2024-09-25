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

package org.opendc.experiments.base.scenario

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import org.opendc.compute.telemetry.export.parquet.ComputeExportConfig
import org.opendc.experiments.base.scenario.specs.ExperimentSpec
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.inputStream

public class ExperimentReader {
    private val jsonReader = Json

    public fun read(file: File): ExperimentSpec = read(file.inputStream())

    public fun read(path: Path): ExperimentSpec = read(path.inputStream())

    /**
     * Read the specified [input].
     */
    @OptIn(ExperimentalSerializationApi::class)
    public fun read(input: InputStream): ExperimentSpec {
        // Loads the default parquet output fields,
        // so that they can be deserialized
        ComputeExportConfig.loadDfltColumns()

        return jsonReader.decodeFromStream<ExperimentSpec>(input)
    }
}
