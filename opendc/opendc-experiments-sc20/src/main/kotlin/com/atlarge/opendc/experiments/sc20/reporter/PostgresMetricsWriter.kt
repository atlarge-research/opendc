/*
 * MIT License
 *
 * Copyright (c) 2020 atlarge-research
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

package com.atlarge.opendc.experiments.sc20.reporter

import de.bytefish.pgbulkinsert.row.SimpleRow
import de.bytefish.pgbulkinsert.row.SimpleRowWriter
import org.postgresql.PGConnection
import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import javax.sql.DataSource

/**
 * The experiment writer is a separate thread that is responsible for writing the results to the
 * database.
 */
public abstract class PostgresMetricsWriter<T>(
    private val ds: DataSource,
    private val parallelism: Int = 8,
    private val bufferSize: Int = 4096
) : Runnable, Closeable {
    /**
     * The queue of commands to process.
     */
    private val queue: BlockingQueue<Action> = ArrayBlockingQueue(parallelism * bufferSize)

    /**
     * The executor service to use.
     */
    private val executorService = Executors.newFixedThreadPool(parallelism)

    /**
     * Write the specified metrics to the database.
     */
    public fun write(scenario: Long, run: Int, metrics: T) {
        queue.put(Action.Write(scenario, run, metrics))
    }

    /**
     * Signal the writer to stop.
     */
    public override fun close() {
        repeat(parallelism) {
            queue.put(Action.Stop)
        }
        executorService.shutdown()
        executorService.awaitTermination(5, TimeUnit.MINUTES)
    }

    /**
     * Create the table to which we write.
     */
    public abstract val table: SimpleRowWriter.Table

    /**
     * Persist the specified metrics to the given [row].
     */
    public abstract fun persist(action: Action.Write<T>, row: SimpleRow)

    init {
        repeat(parallelism) {
            executorService.submit { run() }
        }
    }

    /**
     * Start the writer thread.
     */
    override fun run() {
        val conn = ds.connection

        val writer = SimpleRowWriter(table)
        writer.open(conn.unwrap(PGConnection::class.java))

        try {

            loop@ while (true) {
                val action = queue.take()
                when (action) {
                    is Action.Stop -> break@loop
                    is Action.Write<*> -> writer.startRow {
                        @Suppress("UNCHECKED_CAST")
                        persist(action as Action.Write<T>, it)
                    }
                }
            }
        } finally {
            writer.close()
            conn.close()
        }
    }

    sealed class Action {
        /**
         * A poison pill that will stop the writer thread.
         */
        object Stop : Action()

        /**
         * Write the specified metrics to the database.
         */
        data class Write<out T>(val scenario: Long, val run: Int, val metrics: T) : Action()
    }
}
