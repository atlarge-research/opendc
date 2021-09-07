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

package org.opendc.experiments.radice.scenario.mapper

import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.annotation.JsonUnwrapped
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinFeature
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.fasterxml.jackson.module.kotlin.readValues
import org.opendc.experiments.radice.scenario.*
import java.io.File

/**
 * Helper class to convert a [ScenarioSpec] from and to YAML.
 */
class YamlScenarioMapper {
    /**
     * The [ObjectMapper] for converting the scenarios.
     */
    private val mapper: ObjectMapper = YAMLMapper()
        .registerModule(JavaTimeModule())
        .registerModule(
            KotlinModule.Builder()
                .withReflectionCacheSize(512)
                .configure(KotlinFeature.NullToEmptyCollection, false)
                .configure(KotlinFeature.NullToEmptyMap, false)
                .configure(KotlinFeature.NullIsSameAsDefault, false)
                .configure(KotlinFeature.StrictNullChecks, false)
                .build()
        )
        .enable(SerializationFeature.INDENT_OUTPUT)

    /**
     * Load a [ScenarioSpec] located at [path].
     */
    fun load(path: File): ScenarioSpec {
        return mapper.readValue<ScenarioWrapper>(path).scenario
    }

    /**
     * Load all [ScenarioSpec]s located at [path].
     */
    fun loadAll(path: File): List<ScenarioSpec> {
        val parser = mapper.createParser(path)
        return parser.use {
            mapper.readValues<ScenarioWrapper>(parser)
                .readAll()
                .map { it.scenario }
        }
    }

    /**
     * Write the specified [scenario].
     */
    fun write(basePath: File, scenario: ScenarioSpec, fitness: List<Double>? = null) {
        val partition = scenario.partitions.map { (k, v) -> "$k=$v" }.joinToString("/")
        val partitionDir = File(basePath, partition)
        partitionDir.mkdirs() // Create all necessary directories
        val scenarioFile = File(partitionDir, "scenario.yml")

        val res = ScenarioWrapper()
        res.scenario = scenario
        res.fitness = fitness

        mapper.writeValue(scenarioFile, res)
    }

    /**
     * Write the specified [scenarios].
     */
    fun writeAll(path: File, scenarios: List<ScenarioSpec>) {
        val writer = mapper.writer().writeValues(path)
        writer.use { writer.writeAll(scenarios) }
    }

    /**
     * The entry to write to JSON.
     */
    private class ScenarioWrapper {
        @JsonUnwrapped
        lateinit var scenario: ScenarioSpec

        @JsonInclude(JsonInclude.Include.NON_NULL)
        var fitness: List<Double>? = null
    }
}
