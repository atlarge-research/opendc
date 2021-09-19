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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.trace.*
import java.net.URL

/**
 * Test suite for the [BitbrainsExTraceFormat] class.
 */
class BitbrainsExTraceFormatTest {
    private val format = BitbrainsExTraceFormat()

    @Test
    fun testTraceExists() {
        val url = checkNotNull(BitbrainsExTraceFormatTest::class.java.getResource("/vm.txt"))
        assertDoesNotThrow {
            format.open(url)
        }
    }

    @Test
    fun testTraceDoesNotExists() {
        val url = checkNotNull(BitbrainsExTraceFormatTest::class.java.getResource("/vm.txt"))
        assertThrows<IllegalArgumentException> {
            format.open(URL(url.toString() + "help"))
        }
    }

    @Test
    fun testTables() {
        val url = checkNotNull(BitbrainsExTraceFormatTest::class.java.getResource("/vm.txt"))
        val trace = format.open(url)

        assertEquals(listOf(TABLE_RESOURCE_STATES), trace.tables)
    }

    @Test
    fun testTableExists() {
        val url = checkNotNull(BitbrainsExTraceFormatTest::class.java.getResource("/vm.txt"))
        val table = format.open(url).getTable(TABLE_RESOURCE_STATES)

        assertNotNull(table)
        assertDoesNotThrow { table!!.newReader() }
    }

    @Test
    fun testTableDoesNotExist() {
        val url = checkNotNull(BitbrainsExTraceFormatTest::class.java.getResource("/vm.txt"))
        val trace = format.open(url)

        assertFalse(trace.containsTable("test"))
        assertNull(trace.getTable("test"))
    }

    @Test
    fun testSmoke() {
        val url = checkNotNull(BitbrainsExTraceFormatTest::class.java.getResource("/vm.txt"))
        val trace = format.open(url)

        val reader = trace.getTable(TABLE_RESOURCE_STATES)!!.newReader()

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals(1631911500, reader.get(RESOURCE_STATE_TIMESTAMP).epochSecond) },
            { assertEquals(21.2, reader.getDouble(RESOURCE_STATE_CPU_USAGE), 0.01) }
        )

        reader.close()
    }
}
