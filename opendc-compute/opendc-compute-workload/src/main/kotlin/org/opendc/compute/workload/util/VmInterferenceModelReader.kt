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

package org.opendc.compute.workload.util

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.opendc.simulator.compute.kernel.interference.VmInterferenceModel
import java.io.File
import java.io.InputStream

/**
 * A parser for the JSON performance interference setup files used for the TPDS article on Capelin.
 */
public class VmInterferenceModelReader {
    /**
     * The [ObjectMapper] to use.
     */
    private val mapper = jacksonObjectMapper()

    /**
     * Read the performance interface model from [file].
     */
    public fun read(file: File): VmInterferenceModel {
        val builder = VmInterferenceModel.builder()
        val parser = mapper.createParser(file)
        parseGroups(parser, builder)
        return builder.build()
    }

    /**
     * Read the performance interface model from the input.
     */
    public fun read(input: InputStream): VmInterferenceModel {
        val builder = VmInterferenceModel.builder()
        val parser = mapper.createParser(input)
        parseGroups(parser, builder)
        return builder.build()
    }

    /**
     * Parse all groups in an interference JSON file.
     */
    private fun parseGroups(parser: JsonParser, builder: VmInterferenceModel.Builder) {
        parser.nextToken()

        if (!parser.isExpectedStartArrayToken) {
            throw JsonParseException(parser, "Expected array at start, but got ${parser.currentToken()}")
        }

        while (parser.nextToken() != JsonToken.END_ARRAY) {
            parseGroup(parser, builder)
        }
    }

    /**
     * Parse a group an interference JSON file.
     */
    private fun parseGroup(parser: JsonParser, builder: VmInterferenceModel.Builder) {
        var targetLoad = Double.POSITIVE_INFINITY
        var score = 1.0
        val members = mutableSetOf<String>()

        if (!parser.isExpectedStartObjectToken) {
            throw JsonParseException(parser, "Expected object, but got ${parser.currentToken()}")
        }

        while (parser.nextValue() != JsonToken.END_OBJECT) {
            when (parser.currentName) {
                "vms" -> parseGroupMembers(parser, members)
                "minServerLoad" -> targetLoad = parser.doubleValue
                "performanceScore" -> score = parser.doubleValue
            }
        }

        builder.addGroup(members, targetLoad, score)
    }

    /**
     * Parse the members of a group.
     */
    private fun parseGroupMembers(parser: JsonParser, members: MutableSet<String>) {
        if (!parser.isExpectedStartArrayToken) {
            throw JsonParseException(parser, "Expected array for group members, but got ${parser.currentToken()}")
        }

        while (parser.nextValue() != JsonToken.END_ARRAY) {
            if (parser.currentToken() != JsonToken.VALUE_STRING) {
                throw JsonParseException(parser, "Expected string value for group member")
            }

            val member = parser.text.removePrefix("vm__workload__").removeSuffix(".txt")
            members.add(member)
        }
    }

    private data class Group(
        @JsonProperty("minServerLoad")
        val targetLoad: Double,
        @JsonProperty("performanceScore")
        val score: Double,
        @JsonProperty("vms")
        val members: Set<String>,
    )
}
