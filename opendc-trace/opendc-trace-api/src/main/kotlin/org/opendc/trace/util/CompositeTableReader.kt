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

package org.opendc.trace.util

import org.opendc.trace.TableReader
import java.time.Duration
import java.time.Instant
import java.util.UUID

/**
 * A helper class to chain multiple [TableReader]s.
 */
public abstract class CompositeTableReader : TableReader {
    /**
     * A flag to indicate that the reader has starting, meaning the user called [nextRow] at least once
     * (and in turn [nextReader]).
     */
    private var hasStarted = false

    /**
     * The active [TableReader] instance.
     */
    private var delegate: TableReader? = null

    /**
     * Obtain the next [TableReader] instance to read from or `null` if there are no more readers to read from.
     */
    protected abstract fun nextReader(): TableReader?

    override fun nextRow(): Boolean {
        tryStart()

        var delegate = delegate

        while (delegate != null) {
            if (delegate.nextRow()) {
                break
            }

            delegate.close()
            delegate = nextReader()
            this.delegate = delegate
        }

        return delegate != null
    }

    override fun resolve(name: String): Int {
        tryStart()

        val delegate = delegate
        return delegate?.resolve(name) ?: -1
    }

    override fun isNull(index: Int): Boolean {
        val delegate = checkNotNull(delegate) { "Invalid reader state" }
        return delegate.isNull(index)
    }

    override fun getBoolean(index: Int): Boolean {
        val delegate = checkNotNull(delegate) { "Invalid reader state" }
        return delegate.getBoolean(index)
    }

    override fun getInt(index: Int): Int {
        val delegate = checkNotNull(delegate) { "Invalid reader state" }
        return delegate.getInt(index)
    }

    override fun getLong(index: Int): Long {
        val delegate = checkNotNull(delegate) { "Invalid reader state" }
        return delegate.getLong(index)
    }

    override fun getFloat(index: Int): Float {
        val delegate = checkNotNull(delegate) { "Invalid reader state" }
        return delegate.getFloat(index)
    }

    override fun getDouble(index: Int): Double {
        val delegate = checkNotNull(delegate) { "Invalid reader state" }
        return delegate.getDouble(index)
    }

    override fun getString(index: Int): String? {
        val delegate = checkNotNull(delegate) { "Invalid reader state" }
        return delegate.getString(index)
    }

    override fun getUUID(index: Int): UUID? {
        val delegate = checkNotNull(delegate) { "Invalid reader state" }
        return delegate.getUUID(index)
    }

    override fun getInstant(index: Int): Instant? {
        val delegate = checkNotNull(delegate) { "Invalid reader state" }
        return delegate.getInstant(index)
    }

    override fun getDuration(index: Int): Duration? {
        val delegate = checkNotNull(delegate) { "Invalid reader state" }
        return delegate.getDuration(index)
    }

    override fun <T> getList(index: Int, elementType: Class<T>): List<T>? {
        val delegate = checkNotNull(delegate) { "Invalid reader state" }
        return delegate.getList(index, elementType)
    }

    override fun <T> getSet(index: Int, elementType: Class<T>): Set<T>? {
        val delegate = checkNotNull(delegate) { "Invalid reader state" }
        return delegate.getSet(index, elementType)
    }

    override fun <K, V> getMap(index: Int, keyType: Class<K>, valueType: Class<V>): Map<K, V>? {
        val delegate = checkNotNull(delegate) { "Invalid reader state" }
        return delegate.getMap(index, keyType, valueType)
    }

    override fun close() {
        delegate?.close()
    }

    override fun toString(): String = "CompositeTableReader"

    /**
     * Try to obtain the initial delegate.
     */
    private fun tryStart() {
        if (!hasStarted) {
            assert(delegate == null) { "Duplicate initialization" }
            delegate = nextReader()
            hasStarted = true
        }
    }
}
