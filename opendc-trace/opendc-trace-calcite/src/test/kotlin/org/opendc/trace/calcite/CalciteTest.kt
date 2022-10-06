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

import io.mockk.every
import io.mockk.mockk
import org.apache.calcite.jdbc.CalciteConnection
import org.junit.jupiter.api.Assertions.assertAll
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.opendc.trace.TableColumn
import org.opendc.trace.TableColumnType
import org.opendc.trace.TableReader
import org.opendc.trace.Trace
import org.opendc.trace.conv.TABLE_RESOURCES
import java.nio.file.Files
import java.nio.file.Paths
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.Statement
import java.sql.Timestamp
import java.time.Duration
import java.time.Instant
import java.util.Properties
import java.util.UUID

/**
 * Smoke test for Apache Calcite integration.
 */
class CalciteTest {
    /**
     * The trace to experiment with.
     */
    private val odcTrace = Trace.open(Paths.get("src/test/resources/trace"), format = "opendc-vm")

    @Test
    fun testResources() {
        runQuery(odcTrace, "SELECT * FROM trace.resources") { rs ->
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
        runQuery(odcTrace, "SELECT * FROM trace.resource_states") { rs ->
            assertAll(
                { assertTrue(rs.next()) },
                { assertEquals("1019", rs.getString("id")) },
                { assertEquals(Timestamp.valueOf("2013-08-12 13:40:46.0"), rs.getTimestamp("timestamp")) },
                { assertEquals(300000, rs.getLong("duration")) },
                { assertEquals(0.0, rs.getDouble("cpu_usage")) },
                { assertTrue(rs.next()) },
                { assertEquals("1019", rs.getString("id")) }
            )
        }
    }

    @Test
    fun testInterferenceGroups() {
        runQuery(odcTrace, "SELECT * FROM trace.interference_groups") { rs ->
            assertAll(
                { assertTrue(rs.next()) },
                { assertArrayEquals(arrayOf("1019", "1023", "1052"), rs.getArray("members").array as Array<*>) },
                { assertEquals(0.0, rs.getDouble("target")) },
                { assertEquals(0.8830158730158756, rs.getDouble("score")) }
            )
        }
    }

    @Test
    fun testComplexQuery() {
        runQuery(odcTrace, "SELECT max(cpu_usage) as max_cpu_usage, avg(cpu_usage) as avg_cpu_usage FROM trace.resource_states") { rs ->
            assertAll(
                { assertTrue(rs.next()) },
                { assertEquals(249.59993808, rs.getDouble("max_cpu_usage")) },
                { assertEquals(5.387240309118493, rs.getDouble("avg_cpu_usage")) }
            )
        }
    }

    @Test
    fun testInsert() {
        val tmp = Files.createTempDirectory("opendc")
        val newTrace = Trace.create(tmp, "opendc-vm")

        runStatement(newTrace) { stmt ->
            val count = stmt.executeUpdate(
                """
                INSERT INTO trace.resources (id, start_time, stop_time, cpu_count, cpu_capacity, mem_capacity)
                VALUES (1234, '2013-08-12 13:35:46.0', '2013-09-11 13:39:58.0', 1, 2926.0, 1024.0)
                """.trimIndent()
            )
            assertEquals(1, count)
        }

        runQuery(newTrace, "SELECT * FROM trace.resources") { rs ->
            assertAll(
                { assertTrue(rs.next()) },
                { assertEquals("1234", rs.getString("id")) },
                { assertEquals(1, rs.getInt("cpu_count")) },
                { assertEquals(Timestamp.valueOf("2013-08-12 13:35:46.0"), rs.getTimestamp("start_time")) },
                { assertEquals(2926.0, rs.getDouble("cpu_capacity")) },
                { assertEquals(1024.0, rs.getDouble("mem_capacity")) }
            )
        }
    }

    @Test
    fun testUUID() {
        val trace = mockk<Trace>()
        every { trace.tables } returns listOf(TABLE_RESOURCES)
        every { trace.getTable(TABLE_RESOURCES)!!.columns } returns listOf(
            TableColumn("id", TableColumnType.UUID)
        )
        every { trace.getTable(TABLE_RESOURCES)!!.newReader() } answers {
            object : TableReader {
                override fun nextRow(): Boolean = true

                override fun resolve(name: String): Int {
                    return when (name) {
                        "id" -> 0
                        else -> -1
                    }
                }

                override fun isNull(index: Int): Boolean = false

                override fun getBoolean(index: Int): Boolean {
                    TODO("not implemented")
                }

                override fun getInt(index: Int): Int {
                    TODO("not implemented")
                }

                override fun getLong(index: Int): Long {
                    TODO("not implemented")
                }

                override fun getFloat(index: Int): Float {
                    TODO("not implemented")
                }

                override fun getDouble(index: Int): Double {
                    TODO("not implemented")
                }

                override fun getString(index: Int): String? {
                    TODO("not implemented")
                }

                override fun getUUID(index: Int): UUID = UUID(1, 2)

                override fun getInstant(index: Int): Instant? {
                    TODO("not implemented")
                }

                override fun getDuration(index: Int): Duration? {
                    TODO("not implemented")
                }

                override fun <T> getList(index: Int, elementType: Class<T>): List<T>? {
                    TODO("not implemented")
                }

                override fun <T> getSet(index: Int, elementType: Class<T>): Set<T>? {
                    TODO("not implemented")
                }

                override fun <K, V> getMap(index: Int, keyType: Class<K>, valueType: Class<V>): Map<K, V>? {
                    TODO("not implemented")
                }

                override fun close() {}
            }
        }

        runQuery(trace, "SELECT id FROM trace.resources") { rs ->
            assertAll(
                { assertTrue(rs.next()) },
                { assertArrayEquals(byteArrayOf(0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 2), rs.getBytes("id")) }
            )
        }
    }

    /**
     * Helper function to run statement for the specified trace.
     */
    private fun runQuery(trace: Trace, query: String, block: (ResultSet) -> Unit) {
        runStatement(trace) { stmt ->
            val rs = stmt.executeQuery(query)
            rs.use { block(rs) }
        }
    }

    /**
     * Helper function to run statement for the specified trace.
     */
    private fun runStatement(trace: Trace, block: (Statement) -> Unit) {
        val info = Properties()
        info.setProperty("lex", "JAVA")
        val connection = DriverManager.getConnection("jdbc:calcite:", info).unwrap(CalciteConnection::class.java)
        connection.rootSchema.add("trace", TraceSchema(trace))

        val stmt = connection.createStatement()
        try {
            block(stmt)
        } finally {
            stmt.close()
            connection.close()
        }
    }
}
