package org.opendc.experiments.scenario


import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File
import java.io.InputStream

public class ScenarioReader {

    @OptIn(ExperimentalSerializationApi::class)
    public fun read(file: File): ScenarioJSONSpec {
        val input = file.inputStream()
        val obj = Json.decodeFromStream<ScenarioJSONSpec>(input)

        return obj
    }

    /**
     * Read the specified [input].
     */
    @OptIn(ExperimentalSerializationApi::class)
    public fun read(input: InputStream): ScenarioJSONSpec {
        val obj = Json.decodeFromStream<ScenarioJSONSpec>(input)
        return obj
    }

}
