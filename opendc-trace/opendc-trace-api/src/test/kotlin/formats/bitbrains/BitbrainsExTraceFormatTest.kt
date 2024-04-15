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

package formats.bitbrains

import formats.wtf.TableReaderTestKit
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.opendc.trace.TableColumn
import org.opendc.trace.TableReader
import org.opendc.trace.bitbrains.BitbrainsExTraceFormat
import org.opendc.trace.conv.TABLE_RESOURCE_STATES
import org.opendc.trace.conv.resourceStateCpuUsage
import org.opendc.trace.conv.resourceStateTimestamp
import java.nio.file.Paths

/**
 * Test suite for the [BitbrainsExTraceFormat] class.
 */
internal class BitbrainsExTraceFormatTest {
    private val format = BitbrainsExTraceFormat()

    @Test
    fun testTables() {
        val path = Paths.get("src/test/resources/bitbrains/vm.txt")

        assertEquals(listOf(TABLE_RESOURCE_STATES), format.getTables(path))
    }

    @Test
    fun testTableExists() {
        val path = Paths.get("src/test/resources/bitbrains/vm.txt")

        assertDoesNotThrow { format.getDetails(path, TABLE_RESOURCE_STATES) }
    }

    @Test
    fun testTableDoesNotExist() {
        val path = Paths.get("src/test/resources/bitbrains/vm.txt")
        assertThrows<IllegalArgumentException> { format.getDetails(path, "test") }
    }

    @Test
    fun testSmoke() {
        val path = Paths.get("src/test/resources/bitbrains/vm.txt")
        val reader = format.newReader(path, TABLE_RESOURCE_STATES, null)

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals(1631911500, reader.getInstant(resourceStateTimestamp)?.epochSecond) },
            { assertEquals(21.2, reader.getDouble(resourceStateCpuUsage), 0.01) },
        )

        reader.close()
    }

    @DisplayName("TableReader for Resource States")
    @Nested
    inner class ResourceStatesTableReaderTest : TableReaderTestKit() {
        override lateinit var reader: TableReader
        override lateinit var columns: List<TableColumn>

        @BeforeEach
        fun setUp() {
            val path = Paths.get("src/test/resources/bitbrains/vm.txt")

            columns = format.getDetails(path, TABLE_RESOURCE_STATES).columns
            reader = format.newReader(path, TABLE_RESOURCE_STATES, null)
        }
    }
}
