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

package org.opendc.trace.calcite

import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.sql.DriverManager
import java.sql.Timestamp
import java.util.Properties

/**
 * Test suite for [TraceSchemaFactory].
 */
class TraceSchemaFactoryTest {
    @Test
    fun testSmoke() {
        val info = Properties()
        info.setProperty("lex", "JAVA")
        val connection = DriverManager.getConnection("jdbc:calcite:model=src/test/resources/model.json", info)
        val stmt = connection.createStatement()
        val rs = stmt.executeQuery("SELECT * FROM trace.resources")
        try {
            assertAll(
                { assertTrue(rs.next()) },
                { assertEquals("1019", rs.getString("id")) },
                { assertEquals(1, rs.getInt("cpu_count")) },
                { assertEquals(Timestamp.valueOf("2013-08-12 13:40:46.0"), rs.getTimestamp("start_time")) },
                { assertEquals(181352.0, rs.getDouble("mem_capacity")) }
            )
        } finally {
            rs.close()
            stmt.close()
            connection.close()
        }
    }

    @Test
    fun testWithoutParams() {
        assertThrows<java.lang.RuntimeException> {
            DriverManager.getConnection("jdbc:calcite:schemaFactory=org.opendc.trace.calcite.TraceSchemaFactory")
        }
    }

    @Test
    fun testWithoutPath() {
        assertThrows<java.lang.RuntimeException> {
            DriverManager.getConnection("jdbc:calcite:schemaFactory=org.opendc.trace.calcite.TraceSchemaFactory; schema.format=opendc-vm")
        }
    }

    @Test
    fun testWithoutFormat() {
        assertThrows<java.lang.RuntimeException> {
            DriverManager.getConnection("jdbc:calcite:schemaFactory=org.opendc.trace.calcite.TraceSchemaFactory; schema.path=trace")
        }
    }
}
