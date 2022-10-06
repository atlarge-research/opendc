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

import com.fasterxml.jackson.core.JsonGenerator
import org.opendc.trace.TableWriter
import org.opendc.trace.conv.INTERFERENCE_GROUP_MEMBERS
import org.opendc.trace.conv.INTERFERENCE_GROUP_SCORE
import org.opendc.trace.conv.INTERFERENCE_GROUP_TARGET
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * A [TableWriter] implementation for the OpenDC VM interference JSON format.
 */
internal class OdcVmInterferenceJsonTableWriter(private val generator: JsonGenerator) : TableWriter {
    /**
     * A flag to indicate whether a row has been started.
     */
    private var isRowActive = false

    init {
        generator.writeStartArray()
    }

    override fun startRow() {
        // Reset state
        members = emptySet()
        targetLoad = Double.POSITIVE_INFINITY
        score = 1.0

        // Mark row as active
        isRowActive = true
    }

    override fun endRow() {
        check(isRowActive) { "No active row" }

        generator.writeStartObject()
        generator.writeArrayFieldStart("vms")
        for (member in members) {
            generator.writeString(member)
        }
        generator.writeEndArray()
        generator.writeNumberField("minServerLoad", targetLoad)
        generator.writeNumberField("performanceScore", score)
        generator.writeEndObject()
    }

    override fun resolve(name: String): Int {
        return when (name) {
            INTERFERENCE_GROUP_MEMBERS -> COL_MEMBERS
            INTERFERENCE_GROUP_TARGET -> COL_TARGET
            INTERFERENCE_GROUP_SCORE -> COL_SCORE
            else -> -1
        }
    }

    override fun setBoolean(index: Int, value: Boolean) {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun setInt(index: Int, value: Int) {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun setLong(index: Int, value: Long) {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun setFloat(index: Int, value: Float) {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun setDouble(index: Int, value: Double) {
        check(isRowActive) { "No active row" }

        when (index) {
            COL_TARGET -> targetLoad = (value as Number).toDouble()
            COL_SCORE -> score = (value as Number).toDouble()
            else -> throw IllegalArgumentException("Invalid column $index")
        }
    }

    override fun setString(index: Int, value: String) {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun setUUID(index: Int, value: UUID) {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun setInstant(index: Int, value: Instant) {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun setDuration(index: Int, value: Duration) {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun <T> setList(index: Int, value: List<T>) {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun <T> setSet(index: Int, value: Set<T>) {
        check(isRowActive) { "No active row" }

        @Suppress("UNCHECKED_CAST")
        when (index) {
            COL_MEMBERS -> members = value as Set<String>
            else -> throw IllegalArgumentException("Invalid column index $index")
        }
    }

    override fun <K, V> setMap(index: Int, value: Map<K, V>) {
        throw IllegalArgumentException("Invalid column $index")
    }

    override fun flush() {
        generator.flush()
    }

    override fun close() {
        generator.writeEndArray()
        generator.close()
    }

    private val COL_MEMBERS = 0
    private val COL_TARGET = 1
    private val COL_SCORE = 2

    private var members = emptySet<String>()
    private var targetLoad = Double.POSITIVE_INFINITY
    private var score = 1.0
}
