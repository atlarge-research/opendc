/*
 * Copyright (c) 2021 AtLarge Research
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

package org.opendc.compute.topology

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.opendc.common.logger.logger
import org.opendc.compute.topology.specs.TopologySpec
import java.io.File
import java.io.InputStream
import java.nio.file.Path
import kotlin.io.path.inputStream

/**
 * A helper class for reading a topology specification file.
 */
public class TopologyReader {
    private val jsonReader = Json { ignoreUnknownKeys = true }
    private val strictJsonReader = Json { ignoreUnknownKeys = false }

    public fun read(
        path: Path,
        strictReader: Boolean = false,
    ): TopologySpec = read(path.inputStream(), strictReader)

    public fun read(
        file: File,
        strictReader: Boolean = false,
    ): TopologySpec = read(file.inputStream(), strictReader)

    /**
     * Read the specified [input].
     */
    public fun read(
        input: InputStream,
        strictReader: Boolean = false,
    ): TopologySpec {
        val text = input.bufferedReader().use { it.readText() }

        if (strictReader) {
            return strictJsonReader.decodeFromString<TopologySpec>(text)
        }

        val topology = jsonReader.decodeFromString<TopologySpec>(text)

        // [jsonReader] ignores unknown keys, so typos and stale fields would otherwise pass silently.
        // Decode once more with a strict reader to surface them: the two readers differ only in
        // [JsonBuilder.ignoreUnknownKeys], and the lenient decode above already succeeded, so any
        // failure here can only be caused by an unknown key that is being ignored.
        try {
            strictJsonReader.decodeFromString<TopologySpec>(text)
        } catch (e: SerializationException) {
            LOG.warn("The topology file contains an unknown key that is ignored: ${e.message?.substringBefore('\n')}")
        }

        return topology
    }

    private companion object {
        private val LOG by logger("org.opendc.compute.topology.TopologyReader")
    }
}
