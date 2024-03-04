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

package org.opendc.trace.azure

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
import org.opendc.trace.conv.TABLE_RESOURCES
import org.opendc.trace.conv.TABLE_RESOURCE_STATES
import org.opendc.trace.conv.resourceCpuCount
import org.opendc.trace.conv.resourceID
import org.opendc.trace.conv.resourceMemCapacity
import org.opendc.trace.conv.resourceStateCpuUsagePct
import org.opendc.trace.conv.resourceStateTimestamp
import org.opendc.trace.testkit.TableReaderTestKit
import java.nio.file.Paths

/**
 * Test suite for the [AzureTraceFormat] class.
 */
@DisplayName("Azure VM TraceFormat")
class AzureTraceFormatTest {
    private val format = AzureTraceFormat()

    @Test
    fun testTables() {
        val path = Paths.get("src/test/resources/trace")

        assertEquals(listOf(TABLE_RESOURCES, TABLE_RESOURCE_STATES), format.getTables(path))
    }

    @Test
    fun testTableExists() {
        val path = Paths.get("src/test/resources/trace")

        assertDoesNotThrow { format.getDetails(path, TABLE_RESOURCE_STATES) }
    }

    @Test
    fun testTableDoesNotExist() {
        val path = Paths.get("src/test/resources/trace")
        assertThrows<IllegalArgumentException> { format.getDetails(path, "test") }
    }

    @Test
    fun testResources() {
        val path = Paths.get("src/test/resources/trace")
        val reader = format.newReader(path, TABLE_RESOURCES, null)
        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("x/XsOfHO4ocsV99i4NluqKDuxctW2MMVmwqOPAlg4wp8mqbBOe3wxBlQo0+Qx+uf", reader.getString(resourceID)) },
            { assertEquals(1, reader.getInt(resourceCpuCount)) },
            { assertEquals(1750000.0, reader.getDouble(resourceMemCapacity)) },
        )

        reader.close()
    }

    @Test
    fun testSmoke() {
        val path = Paths.get("src/test/resources/trace")
        val reader = format.newReader(path, TABLE_RESOURCE_STATES, null)

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("+ZcrOp5/c/fJ6mVgP5qMZlOAGDwyjaaDNM0WoWOt2IDb47gT0UwK9lFwkPQv3C7Q", reader.getString(resourceID)) },
            { assertEquals(0, reader.getInstant(resourceStateTimestamp)?.epochSecond) },
            { assertEquals(0.0286979, reader.getDouble(resourceStateCpuUsagePct), 0.01) },
        )

        reader.close()
    }

    @DisplayName("TableReader for Resources")
    @Nested
    inner class ResourcesTableReaderTest : TableReaderTestKit() {
        override lateinit var reader: TableReader
        override lateinit var columns: List<TableColumn>

        @BeforeEach
        fun setUp() {
            val path = Paths.get("src/test/resources/trace")

            columns = format.getDetails(path, TABLE_RESOURCES).columns
            reader = format.newReader(path, TABLE_RESOURCES, null)
        }
    }

    @DisplayName("TableReader for Resource States")
    @Nested
    inner class ResourceStatesTableReaderTest : TableReaderTestKit() {
        override lateinit var reader: TableReader
        override lateinit var columns: List<TableColumn>

        @BeforeEach
        fun setUp() {
            val path = Paths.get("src/test/resources/trace")

            columns = format.getDetails(path, TABLE_RESOURCE_STATES).columns
            reader = format.newReader(path, TABLE_RESOURCE_STATES, null)
        }
    }
}
