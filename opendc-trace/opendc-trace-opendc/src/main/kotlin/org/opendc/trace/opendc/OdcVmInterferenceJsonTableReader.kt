/*
 * Copyright (c) 2022 AtLarge Research
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

package org.opendc.trace.opendc

import com.fasterxml.jackson.core.JsonParseException
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.JsonToken
import org.opendc.trace.TableColumnType
import org.opendc.trace.TableReader
import org.opendc.trace.conv.INTERFERENCE_GROUP_MEMBERS
import org.opendc.trace.conv.INTERFERENCE_GROUP_SCORE
import org.opendc.trace.conv.INTERFERENCE_GROUP_TARGET
import org.opendc.trace.util.convertTo
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * A [TableReader] implementation for the OpenDC VM interference JSON format.
 */
internal class OdcVmInterferenceJsonTableReader(private val parser: JsonParser) : TableReader {
    /**
     * A flag to indicate whether a single row has been read already.
     */
    private var isStarted = false

    override fun nextRow(): Boolean {
        if (!isStarted) {
            isStarted = true

            parser.nextToken()

            if (!parser.isExpectedStartArrayToken) {
                throw JsonParseException(parser, "Expected array at start, but got ${parser.currentToken()}")
            }
        }

        return if (parser.isClosed || parser.nextToken() == JsonToken.END_ARRAY) {
            parser.close()
            reset()
            false
        } else {
            parseGroup(parser)
            true
        }
    }

    private val COL_MEMBERS = 0
    private val COL_TARGET = 1
    private val COL_SCORE = 2

    private val TYPE_MEMBERS = TableColumnType.Set(TableColumnType.String)

    override fun resolve(name: String): Int {
        return when (name) {
            INTERFERENCE_GROUP_MEMBERS -> COL_MEMBERS
            INTERFERENCE_GROUP_TARGET -> COL_TARGET
            INTERFERENCE_GROUP_SCORE -> COL_SCORE
            else -> -1
        }
    }

    override fun isNull(index: Int): Boolean {
        return when (index) {
            COL_MEMBERS, COL_TARGET, COL_SCORE -> false
            else -> throw IllegalArgumentException("Invalid column index $index")
        }
    }

    override fun getBoolean(index: Int): Boolean {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun getInt(index: Int): Int {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun getLong(index: Int): Long {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun getFloat(index: Int): Float {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun getDouble(index: Int): Double {
        checkActive()
        return when (index) {
            COL_TARGET -> targetLoad
            COL_SCORE -> score
            else -> throw IllegalArgumentException("Invalid column $index")
        }
    }

    override fun getString(index: Int): String? {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun getUUID(index: Int): UUID? {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun getInstant(index: Int): Instant? {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun getDuration(index: Int): Duration? {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun <T> getList(index: Int, elementType: Class<T>): List<T>? {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun <T> getSet(index: Int, elementType: Class<T>): Set<T>? {
        checkActive()
        return when (index) {
            COL_MEMBERS -> TYPE_MEMBERS.convertTo(members, elementType)
            else -> throw IllegalArgumentException("Invalid column $index")
        }
    }

    override fun <K, V> getMap(index: Int, keyType: Class<K>, valueType: Class<V>): Map<K, V>? {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun close() {
        parser.close()
    }

    private var members = emptySet<String>()
    private var targetLoad = Double.POSITIVE_INFINITY
    private var score = 1.0

    /**
     * Helper method to check if the reader is active.
     */
    private fun checkActive() {
        check(isStarted && !parser.isClosed) { "No active row. Did you call nextRow()?" }
    }

    /**
     * Reset the state.
     */
    private fun reset() {
        members = emptySet()
        targetLoad = Double.POSITIVE_INFINITY
        score = 1.0
    }

    /**
     * Parse a group an interference JSON file.
     */
    private fun parseGroup(parser: JsonParser) {
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

        this.members = members
        this.targetLoad = targetLoad
        this.score = score
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

            members.add(parser.text)
        }
    }
}
