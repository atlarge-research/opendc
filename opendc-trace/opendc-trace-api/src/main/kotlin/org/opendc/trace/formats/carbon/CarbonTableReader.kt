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

package org.opendc.trace.formats.carbon

import org.opendc.trace.TableReader
import org.opendc.trace.conv.CARBON_INTENSITY
import org.opendc.trace.conv.CARBON_TIMESTAMP
import org.opendc.trace.formats.carbon.parquet.CarbonFragment
import org.opendc.trace.util.parquet.LocalParquetReader
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * A [TableReader] implementation for the WTF format.
 */
internal class CarbonTableReader(private val reader: LocalParquetReader<CarbonFragment>) : TableReader {
    /**
     * The current record.
     */
    private var record: CarbonFragment? = null

    override fun nextRow(): Boolean {
        try {
            val record = reader.read()
            this.record = record

            return record != null
        } catch (e: Throwable) {
            this.record = null
            throw e
        }
    }

    private val colTimestamp = 0
    private val colCarbonIntensity = 1

    override fun resolve(name: String): Int {
        return when (name) {
            CARBON_TIMESTAMP -> colTimestamp
            CARBON_INTENSITY -> colCarbonIntensity
            else -> -1
        }
    }

    override fun isNull(index: Int): Boolean {
        require(index in colTimestamp..colCarbonIntensity) { "Invalid column index" }
        return false
    }

    override fun getBoolean(index: Int): Boolean {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInt(index: Int): Int {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getLong(index: Int): Long {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getFloat(index: Int): Float {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getDouble(index: Int): Double {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (index) {
            colCarbonIntensity -> record.carbonIntensity
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getString(index: Int): String {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getUUID(index: Int): UUID? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInstant(index: Int): Instant {
        val record = checkNotNull(record) { "Reader in invalid state" }
        return when (index) {
            colTimestamp -> record.timestamp
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getDuration(index: Int): Duration {
        throw IllegalArgumentException("Invalid column")
    }

    override fun <T> getList(
        index: Int,
        elementType: Class<T>,
    ): List<T>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun <T> getSet(
        index: Int,
        elementType: Class<T>,
    ): Set<T>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun <K, V> getMap(
        index: Int,
        keyType: Class<K>,
        valueType: Class<V>,
    ): Map<K, V>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun close() {
        reader.close()
    }
}
