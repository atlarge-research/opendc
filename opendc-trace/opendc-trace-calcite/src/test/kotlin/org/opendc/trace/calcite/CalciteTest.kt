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

import org.apache.calcite.jdbc.CalciteConnection
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.opendc.trace.Trace
import java.nio.file.Paths
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Timestamp
import java.util.*

/**
 * Smoke test for Apache Calcite integration.
 */
class CalciteTest {
    /**
     * The trace to experiment with.
     */
    private val trace = Trace.open(Paths.get("src/test/resources/trace"), format = "opendc-vm")

    @Test
    fun testResources() {
        runQuery(trace, "SELECT * FROM trace.resources") { rs ->
            assertAll(
                { assertTrue(rs.next()) },
                { assertEquals("1019", rs.getString("id")) },
                { assertEquals(1, rs.getInt("cpu_count")) },
                { assertEquals(Timestamp.valueOf("2013-08-12 13:40:46.0"), rs.getTimestamp("start_time")) },
                { assertEquals(181352.0, rs.getDouble("mem_capacity")) },
                { assertTrue(rs.next()) },
                { assertEquals("1023", rs.getString("id")) },
                { assertTrue(rs.next()) },
                { assertEquals("1052", rs.getString("id")) },
                { assertTrue(rs.next()) },
                { assertEquals("1073", rs.getString("id")) },
                { assertFalse(rs.next()) }
            )
        }
    }

    @Test
    fun testResourceStates() {
        runQuery(trace, "SELECT * FROM trace.resource_states") { rs ->
            assertAll(
                { assertTrue(rs.next()) },
                { assertEquals("1019", rs.getString("id")) },
                { assertEquals(Timestamp.valueOf("2013-08-12 13:40:46.0"), rs.getTimestamp("timestamp")) },
                { assertEquals(300000, rs.getLong("duration")) },
                { assertEquals(0.0, rs.getDouble("cpu_usage")) },
                { assertTrue(rs.next()) },
                { assertEquals("1019", rs.getString("id")) },
            )
        }
    }

    @Test
    fun testInterferenceGroups() {
        runQuery(trace, "SELECT * FROM trace.interference_groups") { rs ->
            assertAll(
                { assertTrue(rs.next()) },
                { assertArrayEquals(arrayOf("1019", "1023", "1052"), rs.getArray("members").array as Array<*>) },
                { assertEquals(0.0, rs.getDouble("target")) },
                { assertEquals(0.8830158730158756, rs.getDouble("score")) },
            )
        }
    }

    @Test
    fun testComplexQuery() {
        runQuery(trace, "SELECT max(cpu_usage) as max_cpu_usage, avg(cpu_usage) as avg_cpu_usage FROM trace.resource_states") { rs ->
            assertAll(
                { assertTrue(rs.next()) },
                { assertEquals(249.59993808, rs.getDouble("max_cpu_usage")) },
                { assertEquals(5.387240309118493, rs.getDouble("avg_cpu_usage")) },
            )
        }
    }

    /**
     * Helper function to run statement for the specified trace.
     */
    private fun runQuery(trace: Trace, query: String, block: (ResultSet) -> Unit) {
        val info = Properties()
        info.setProperty("lex", "JAVA")
        val connection = DriverManager.getConnection("jdbc:calcite:", info).unwrap(CalciteConnection::class.java)
        connection.rootSchema.add("trace", TraceSchema(trace))

        val stmt = connection.createStatement()
        val results = stmt.executeQuery(query)
        try {
            block(results)
        } finally {
            results.close()
            stmt.close()
            connection.close()
        }
    }
}
