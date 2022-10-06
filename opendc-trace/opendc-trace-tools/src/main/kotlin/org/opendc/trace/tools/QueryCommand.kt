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

package org.opendc.trace.tools

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.options.option
import com.github.ajalt.clikt.parameters.options.required
import com.github.ajalt.clikt.parameters.types.file
import org.apache.calcite.jdbc.CalciteConnection
import org.jline.builtins.Styles
import org.jline.console.Printer
import org.jline.console.impl.DefaultPrinter
import org.jline.terminal.Terminal
import org.jline.terminal.TerminalBuilder
import org.jline.utils.AttributedStringBuilder
import org.opendc.trace.Trace
import org.opendc.trace.calcite.TraceSchema
import java.nio.charset.StandardCharsets
import java.sql.DriverManager
import java.sql.ResultSet
import java.sql.ResultSetMetaData
import java.util.Properties

/**
 * A [CliktCommand] that allows users to query workload traces using SQL.
 */
internal class QueryCommand : CliktCommand(name = "query", help = "Query workload traces") {
    /**
     * The trace to open.
     */
    private val input by option("-i", "--input")
        .file(mustExist = true)
        .required()

    /**
     * The input format of the trace.
     */
    private val inputFormat by option("-f", "--format", help = "format of the trace")
        .required()

    /**
     * The query to execute.
     */
    private val query by argument()

    /**
     * Access to the terminal.
     */
    private val terminal = TerminalBuilder.builder()
        .system(false)
        .streams(System.`in`, System.out)
        .encoding(StandardCharsets.UTF_8)
        .build()

    /**
     * Helper class to print results to console.
     */
    private val printer = QueryPrinter(terminal)

    override fun run() {
        val inputTrace = Trace.open(input, format = inputFormat)
        val info = Properties().apply { this["lex"] = "JAVA" }
        val connection = DriverManager.getConnection("jdbc:calcite:", info).unwrap(CalciteConnection::class.java)
        connection.rootSchema.add("trace", TraceSchema(inputTrace))
        connection.schema = "trace"

        val stmt = connection.createStatement()
        stmt.executeQuery(query)

        val start = System.currentTimeMillis()
        val hasResults = stmt.execute(query)

        try {
            if (hasResults) {
                do {
                    stmt.resultSet.use { rs ->
                        val count: Int = printResults(rs)
                        val duration = (System.currentTimeMillis() - start) / 1000.0
                        printer.println("$count rows selected (${"%.3f".format(duration)} seconds)")
                    }
                } while (stmt.moreResults)
            } else {
                val count: Int = stmt.updateCount
                val duration = (System.currentTimeMillis() - start) / 1000.0

                printer.println("$count rows affected (${"%0.3f".format(duration)} seconds)")
            }
        } finally {
            stmt.close()
            connection.close()
        }
    }

    /**
     * Helper function to print the results to console.
     */
    private fun printResults(rs: ResultSet): Int {
        var count = 0
        val meta: ResultSetMetaData = rs.metaData

        val options = mapOf(
            Printer.COLUMNS to List(meta.columnCount) { meta.getColumnName(it + 1) },
            Printer.BORDER to "|"
        )
        val data = mutableListOf<Map<String, Any>>()

        while (rs.next()) {
            val row = mutableMapOf<String, Any>()
            for (i in 1..meta.columnCount) {
                row[meta.getColumnName(i)] = rs.getObject(i)
            }
            data.add(row)

            count++
        }

        printer.println(options, data)

        return count
    }

    /**
     * Helper class to print the results of the query.
     */
    private class QueryPrinter(private val terminal: Terminal) : DefaultPrinter(null) {
        override fun terminal(): Terminal = terminal

        override fun highlightAndPrint(options: MutableMap<String, Any>, exception: Throwable) {
            if (options.getOrDefault("exception", "stack") == "stack") {
                exception.printStackTrace()
            } else {
                val asb = AttributedStringBuilder()
                asb.append(exception.message, Styles.prntStyle().resolve(".em"))
                asb.toAttributedString().println(terminal())
            }
        }
    }
}
