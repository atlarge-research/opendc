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

package org.opendc.trace.bitbrains

import com.fasterxml.jackson.dataformat.csv.CsvFactory
import org.opendc.trace.TableReader
import org.opendc.trace.conv.RESOURCE_ID
import java.nio.file.Path
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * A [TableReader] for the Bitbrains resource table.
 */
internal class BitbrainsResourceTableReader(private val factory: CsvFactory, vms: Map<String, Path>) : TableReader {
    /**
     * An iterator to iterate over the resource entries.
     */
    private val it = vms.iterator()

    /**
     * The state of the reader.
     */
    private var state = State.Pending

    override fun nextRow(): Boolean {
        if (state == State.Pending) {
            state = State.Active
        }

        reset()

        while (it.hasNext()) {
            val (name, path) = it.next()

            val parser = factory.createParser(path.toFile())
            val reader = BitbrainsResourceStateTableReader(name, parser)
            val idCol = reader.resolve(RESOURCE_ID)

            try {
                if (!reader.nextRow()) {
                    continue
                }

                id = reader.getString(idCol)
                return true
            } finally {
                reader.close()
            }
        }

        state = State.Closed
        return false
    }

    private val COL_ID = 0

    override fun resolve(name: String): Int {
        return when (name) {
            RESOURCE_ID -> COL_ID
            else -> -1
        }
    }

    override fun isNull(index: Int): Boolean {
        require(index in 0..COL_ID) { "Invalid column index" }
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
        throw IllegalArgumentException("Invalid column")
    }

    override fun getString(index: Int): String? {
        check(state == State.Active) { "No active row" }
        return when (index) {
            COL_ID -> id
            else -> throw IllegalArgumentException("Invalid column")
        }
    }

    override fun getUUID(index: Int): UUID? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getInstant(index: Int): Instant? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun getDuration(index: Int): Duration? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun <T> getList(index: Int, elementType: Class<T>): List<T>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun <T> getSet(index: Int, elementType: Class<T>): Set<T>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun <K, V> getMap(index: Int, keyType: Class<K>, valueType: Class<V>): Map<K, V>? {
        throw IllegalArgumentException("Invalid column")
    }

    override fun close() {
        reset()
        state = State.Closed
    }

    /**
     * State fields of the reader.
     */
    private var id: String? = null

    /**
     * Reset the state of the reader.
     */
    private fun reset() {
        id = null
    }

    private enum class State {
        Pending, Active, Closed
    }
}
