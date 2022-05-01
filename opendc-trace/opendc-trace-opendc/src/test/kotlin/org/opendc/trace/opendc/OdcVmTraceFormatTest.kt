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

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import org.opendc.trace.conv.*
import java.nio.file.Files
import java.nio.file.Paths
import java.time.Instant

/**
 * Test suite for the [OdcVmTraceFormat] implementation.
 */
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
        val reader = format.newReader(path, TABLE_RESOURCES)

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("1019", reader.get(RESOURCE_ID)) },
            { assertTrue(reader.nextRow()) },
            { assertEquals("1023", reader.get(RESOURCE_ID)) },
            { assertTrue(reader.nextRow()) },
            { assertEquals("1052", reader.get(RESOURCE_ID)) },
            { assertTrue(reader.nextRow()) },
            { assertEquals("1073", reader.get(RESOURCE_ID)) },
            { assertFalse(reader.nextRow()) }
        )

        reader.close()
    }

    @Test
    fun testResourcesWrite() {
        val path = Files.createTempDirectory("opendc")
        val writer = format.newWriter(path, TABLE_RESOURCES)

        writer.startRow()
        writer.set(RESOURCE_ID, "1019")
        writer.set(RESOURCE_START_TIME, Instant.EPOCH)
        writer.set(RESOURCE_STOP_TIME, Instant.EPOCH)
        writer.setInt(RESOURCE_CPU_COUNT, 1)
        writer.setDouble(RESOURCE_CPU_CAPACITY, 1024.0)
        writer.setDouble(RESOURCE_MEM_CAPACITY, 1024.0)
        writer.endRow()
        writer.close()

        val reader = format.newReader(path, TABLE_RESOURCES)

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("1019", reader.get(RESOURCE_ID)) },
            { assertEquals(Instant.EPOCH, reader.get(RESOURCE_START_TIME)) },
            { assertEquals(Instant.EPOCH, reader.get(RESOURCE_STOP_TIME)) },
            { assertEquals(1, reader.getInt(RESOURCE_CPU_COUNT)) },
            { assertEquals(1024.0, reader.getDouble(RESOURCE_CPU_CAPACITY)) },
            { assertEquals(1024.0, reader.getDouble(RESOURCE_MEM_CAPACITY)) },
            { assertFalse(reader.nextRow()) },
        )

        reader.close()
    }

    @ParameterizedTest
    @ValueSource(strings = ["trace-v2.0", "trace-v2.1"])
    fun testSmoke(name: String) {
        val path = Paths.get("src/test/resources/$name")
        val reader = format.newReader(path, TABLE_RESOURCE_STATES)

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("1019", reader.get(RESOURCE_ID)) },
            { assertEquals(1376314846, reader.get(RESOURCE_STATE_TIMESTAMP).epochSecond) },
            { assertEquals(0.0, reader.getDouble(RESOURCE_STATE_CPU_USAGE), 0.01) }
        )

        reader.close()
    }

    @Test
    fun testResourceStatesWrite() {
        val path = Files.createTempDirectory("opendc")
        val writer = format.newWriter(path, TABLE_RESOURCE_STATES)

        writer.startRow()
        writer.set(RESOURCE_ID, "1019")
        writer.set(RESOURCE_STATE_TIMESTAMP, Instant.EPOCH)
        writer.setDouble(RESOURCE_STATE_CPU_USAGE, 23.0)
        writer.setInt(RESOURCE_CPU_COUNT, 1)
        writer.endRow()
        writer.close()

        val reader = format.newReader(path, TABLE_RESOURCE_STATES)

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals("1019", reader.get(RESOURCE_ID)) },
            { assertEquals(Instant.EPOCH, reader.get(RESOURCE_STATE_TIMESTAMP)) },
            { assertEquals(1, reader.getInt(RESOURCE_CPU_COUNT)) },
            { assertEquals(23.0, reader.getDouble(RESOURCE_STATE_CPU_USAGE)) },
            { assertFalse(reader.nextRow()) },
        )

        reader.close()
    }

    @Test
    fun testInterferenceGroups() {
        val path = Paths.get("src/test/resources/trace-v2.1")
        val reader = format.newReader(path, TABLE_INTERFERENCE_GROUPS)

        assertAll(
            { assertTrue(reader.nextRow()) },
            { assertEquals(setOf("1019", "1023", "1052"), reader.get(INTERFERENCE_GROUP_MEMBERS)) },
            { assertEquals(0.0, reader.get(INTERFERENCE_GROUP_TARGET)) },
            { assertEquals(0.8830158730158756, reader.get(INTERFERENCE_GROUP_SCORE)) },
            { assertTrue(reader.nextRow()) },
            { assertEquals(setOf("1023", "1052", "1073"), reader.get(INTERFERENCE_GROUP_MEMBERS)) },
            { assertEquals(0.0, reader.get(INTERFERENCE_GROUP_TARGET)) },
            { assertEquals(0.7133055555552751, reader.get(INTERFERENCE_GROUP_SCORE)) },
            { assertFalse(reader.nextRow()) }
        )

        reader.close()
    }

    @Test
    fun testInterferenceGroupsEmpty() {
        val path = Paths.get("src/test/resources/trace-v2.0")
        val reader = format.newReader(path, TABLE_INTERFERENCE_GROUPS)

        assertFalse(reader.nextRow())
        reader.close()
    }
}
