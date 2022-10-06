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

package org.opendc.trace.opendc

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.opendc.trace.TableColumn
import org.opendc.trace.TableReader
import org.opendc.trace.TableWriter
import org.opendc.trace.conv.INTERFERENCE_GROUP_MEMBERS
import org.opendc.trace.conv.INTERFERENCE_GROUP_SCORE
import org.opendc.trace.conv.INTERFERENCE_GROUP_TARGET
import org.opendc.trace.conv.RESOURCE_CPU_CAPACITY
import org.opendc.trace.conv.RESOURCE_CPU_COUNT
import org.opendc.trace.conv.RESOURCE_ID
import org.opendc.trace.conv.RESOURCE_MEM_CAPACITY
import org.opendc.trace.conv.RESOURCE_START_TIME
import org.opendc.trace.conv.RESOURCE_STATE_CPU_USAGE
import org.opendc.trace.conv.RESOURCE_STATE_TIMESTAMP
import org.opendc.trace.conv.RESOURCE_STOP_TIME
import org.opendc.trace.conv.TABLE_INTERFERENCE_GROUPS
import org.opendc.trace.conv.TABLE_RESOURCES
import org.opendc.trace.conv.TABLE_RESOURCE_STATES
import org.opendc.trace.testkit.TableReaderTestKit
import org.opendc.trace.testkit.TableWriterTestKit
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant

/**
 * Test suite for the [OdcVmTraceFormat] implementation.
 */
@DisplayName("OdcVmTraceFormat")
internal class OdcVmTraceFormatTest {
    private val format = OdcVmTraceFormat()

    @Test
    fun testTables() {
        val path = Paths.get("src/test/resources/trace-v2.1")

        assertEquals(listOf(TABLE_RESOURCES, TABLE_RESOURCE_STATES, TABLE_INTERFERENCE_GROUPS), format.getTables(path))
    }

    @Test
    fun testTableExists() {
        val path = Paths.get("src/test/resources/trace-v2.1")

        assertDoesNotThrow { format.getDetails(path, TABLE_RESOURCE_STATES) }
    }

    @Test
    fun testTableDoesNotExist() {
        val path = Paths.get("src/test/resources/trace-v2.1")
        assertThrows<IllegalArgumentException> { format.getDetails(path, "test") }
    }

    @ParameterizedTest
    @ValueSource(strings = ["trace-v2.0", "trace-v2.1"])
    fun testResources(name: String) {
        val path = Paths.get("src/test/resources/$name")
        val reader = format.newReader(path, TABLE_RESOURCES, listOf(RESOURCE_ID, RESOURCE_START_TIME))

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("1019", reader.getString(RESOURCE_ID)) },
            { assertEquals(Instant.ofEpochMilli(1376314846000), reader.getInstant(RESOURCE_START_TIME)) },
            { assertTrue(reader.nextRow()) },
            { assertEquals("1023", reader.getString(RESOURCE_ID)) },
            { assertTrue(reader.nextRow()) },
            { assertEquals("1052", reader.getString(RESOURCE_ID)) },
            { assertTrue(reader.nextRow()) },
            { assertEquals("1073", reader.getString(RESOURCE_ID)) },
            { assertFalse(reader.nextRow()) }
        )

        reader.close()
    }

    @Test
    fun testResourcesWrite() {
        val path = Files.createTempDirectory("opendc")
        val writer = format.newWriter(path, TABLE_RESOURCES)

        writer.startRow()
        writer.setString(RESOURCE_ID, "1019")
        writer.setInstant(RESOURCE_START_TIME, Instant.EPOCH)
        writer.setInstant(RESOURCE_STOP_TIME, Instant.EPOCH)
        writer.setInt(RESOURCE_CPU_COUNT, 1)
        writer.setDouble(RESOURCE_CPU_CAPACITY, 1024.0)
        writer.setDouble(RESOURCE_MEM_CAPACITY, 1024.0)
        writer.endRow()
        writer.close()

        val reader = format.newReader(path, TABLE_RESOURCES, null)

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("1019", reader.getString(RESOURCE_ID)) },
            { assertEquals(Instant.EPOCH, reader.getInstant(RESOURCE_START_TIME)) },
            { assertEquals(Instant.EPOCH, reader.getInstant(RESOURCE_STOP_TIME)) },
            { assertEquals(1, reader.getInt(RESOURCE_CPU_COUNT)) },
            { assertEquals(1024.0, reader.getDouble(RESOURCE_CPU_CAPACITY)) },
            { assertEquals(1024.0, reader.getDouble(RESOURCE_MEM_CAPACITY)) },
            { assertFalse(reader.nextRow()) }
        )

        reader.close()
    }

    @ParameterizedTest
    @ValueSource(strings = ["trace-v2.0", "trace-v2.1"])
    fun testSmoke(name: String) {
        val path = Paths.get("src/test/resources/$name")
        val reader = format.newReader(
            path,
            TABLE_RESOURCE_STATES,
            listOf(RESOURCE_ID, RESOURCE_STATE_TIMESTAMP, RESOURCE_STATE_CPU_USAGE)
        )

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("1019", reader.getString(RESOURCE_ID)) },
            { assertEquals(1376314846, reader.getInstant(RESOURCE_STATE_TIMESTAMP)?.epochSecond) },
            { assertEquals(0.0, reader.getDouble(RESOURCE_STATE_CPU_USAGE), 0.01) }
        )

        reader.close()
    }

    @Test
    fun testResourceStatesWrite() {
        val path = Files.createTempDirectory("opendc")
        val writer = format.newWriter(path, TABLE_RESOURCE_STATES)

        writer.startRow()
        writer.setString(RESOURCE_ID, "1019")
        writer.setInstant(RESOURCE_STATE_TIMESTAMP, Instant.EPOCH)
        writer.setDouble(RESOURCE_STATE_CPU_USAGE, 23.0)
        writer.setInt(RESOURCE_CPU_COUNT, 1)
        writer.endRow()
        writer.close()

        val reader = format.newReader(path, TABLE_RESOURCE_STATES, null)

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("1019", reader.getString(RESOURCE_ID)) },
            { assertEquals(Instant.EPOCH, reader.getInstant(RESOURCE_STATE_TIMESTAMP)) },
            { assertEquals(1, reader.getInt(RESOURCE_CPU_COUNT)) },
            { assertEquals(23.0, reader.getDouble(RESOURCE_STATE_CPU_USAGE)) },
            { assertFalse(reader.nextRow()) }
        )

        reader.close()
    }

    @Test
    fun testInterferenceGroups() {
        val path = Paths.get("src/test/resources/trace-v2.1")
        val reader = format.newReader(
            path,
            TABLE_INTERFERENCE_GROUPS,
            listOf(INTERFERENCE_GROUP_MEMBERS, INTERFERENCE_GROUP_TARGET, INTERFERENCE_GROUP_SCORE)
        )

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals(setOf("1019", "1023", "1052"), reader.getSet(INTERFERENCE_GROUP_MEMBERS, String::class.java)) },
            { assertEquals(0.0, reader.getDouble(INTERFERENCE_GROUP_TARGET)) },
            { assertEquals(0.8830158730158756, reader.getDouble(INTERFERENCE_GROUP_SCORE)) },
            { assertTrue(reader.nextRow()) },
            { assertEquals(setOf("1023", "1052", "1073"), reader.getSet(INTERFERENCE_GROUP_MEMBERS, String::class.java)) },
            { assertEquals(0.0, reader.getDouble(INTERFERENCE_GROUP_TARGET)) },
            { assertEquals(0.7133055555552751, reader.getDouble(INTERFERENCE_GROUP_SCORE)) },
            { assertFalse(reader.nextRow()) }
        )

        reader.close()
    }

    @Test
    fun testInterferenceGroupsEmpty() {
        val path = Paths.get("src/test/resources/trace-v2.0")
        val reader = format.newReader(path, TABLE_INTERFERENCE_GROUPS, listOf(INTERFERENCE_GROUP_MEMBERS))

        assertFalse(reader.nextRow())
        reader.close()
    }

    @Test
    fun testInterferenceGroupsWrite() {
        val path = Files.createTempDirectory("opendc")
        val writer = format.newWriter(path, TABLE_INTERFERENCE_GROUPS)

        writer.startRow()
        writer.setSet(INTERFERENCE_GROUP_MEMBERS, setOf("a", "b", "c"))
        writer.setDouble(INTERFERENCE_GROUP_TARGET, 0.5)
        writer.setDouble(INTERFERENCE_GROUP_SCORE, 0.8)
        writer.endRow()
        writer.flush()

        writer.startRow()
        writer.setSet(INTERFERENCE_GROUP_MEMBERS, setOf("a", "b", "d"))
        writer.setDouble(INTERFERENCE_GROUP_TARGET, 0.5)
        writer.setDouble(INTERFERENCE_GROUP_SCORE, 0.9)
        writer.endRow()
        writer.close()

        val reader = format.newReader(path, TABLE_INTERFERENCE_GROUPS, null)

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals(setOf("a", "b", "c"), reader.getSet(INTERFERENCE_GROUP_MEMBERS, String::class.java)) },
            { assertEquals(0.5, reader.getDouble(INTERFERENCE_GROUP_TARGET)) },
            { assertEquals(0.8, reader.getDouble(INTERFERENCE_GROUP_SCORE)) },
            { assertTrue(reader.nextRow()) },
            { assertEquals(setOf("a", "b", "d"), reader.getSet(INTERFERENCE_GROUP_MEMBERS, String::class.java)) },
            { assertEquals(0.5, reader.getDouble(INTERFERENCE_GROUP_TARGET)) },
            { assertEquals(0.9, reader.getDouble(INTERFERENCE_GROUP_SCORE)) },
            { assertFalse(reader.nextRow()) }
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
            val path = Paths.get("src/test/resources/trace-v2.1")

            columns = format.getDetails(path, TABLE_RESOURCES).columns
            reader = format.newReader(path, TABLE_RESOURCES, null)
        }
    }

    @DisplayName("TableWriter for Resources")
    @Nested
    inner class ResourcesTableWriterTest : TableWriterTestKit() {
        override lateinit var writer: TableWriter
        override lateinit var columns: List<TableColumn>

        @BeforeEach
        fun setUp() {
            val path = Files.createTempDirectory("opendc")

            columns = format.getDetails(Paths.get("src/test/resources/trace-v2.1"), TABLE_RESOURCES).columns
            writer = format.newWriter(path, TABLE_RESOURCES)
        }
    }

    @DisplayName("TableReader for Resource States")
    @Nested
    inner class ResourceStatesTableReaderTest : TableReaderTestKit() {
        override lateinit var reader: TableReader
        override lateinit var columns: List<TableColumn>

        @BeforeEach
        fun setUp() {
            val path = Paths.get("src/test/resources/trace-v2.1")

            columns = format.getDetails(path, TABLE_RESOURCE_STATES).columns
            reader = format.newReader(path, TABLE_RESOURCE_STATES, null)
        }
    }

    @DisplayName("TableWriter for Resource States")
    @Nested
    inner class ResourceStatesTableWriterTest : TableWriterTestKit() {
        override lateinit var writer: TableWriter
        override lateinit var columns: List<TableColumn>

        @BeforeEach
        fun setUp() {
            val path = Files.createTempDirectory("opendc")

            columns = format.getDetails(Paths.get("src/test/resources/trace-v2.1"), TABLE_RESOURCE_STATES).columns
            writer = format.newWriter(path, TABLE_RESOURCE_STATES)
        }
    }

    @DisplayName("TableReader for Interference Groups")
    @Nested
    inner class InterferenceGroupsTableReaderTest : TableReaderTestKit() {
        override lateinit var reader: TableReader
        override lateinit var columns: List<TableColumn>

        @BeforeEach
        fun setUp() {
            val path = Paths.get("src/test/resources/trace-v2.1")

            columns = format.getDetails(path, TABLE_INTERFERENCE_GROUPS).columns
            reader = format.newReader(path, TABLE_INTERFERENCE_GROUPS, null)
        }
    }

    @DisplayName("TableWriter for Interference Groups")
    @Nested
    inner class InterferenceGroupsTableWriterTest : TableWriterTestKit() {
        override lateinit var writer: TableWriter
        override lateinit var columns: List<TableColumn>

        @BeforeEach
        fun setUp() {
            val path = Files.createTempDirectory("opendc")

            columns = format.getDetails(Paths.get("src/test/resources/trace-v2.1"), TABLE_INTERFERENCE_GROUPS).columns
            writer = format.newWriter(path, TABLE_INTERFERENCE_GROUPS)
        }
    }
}
