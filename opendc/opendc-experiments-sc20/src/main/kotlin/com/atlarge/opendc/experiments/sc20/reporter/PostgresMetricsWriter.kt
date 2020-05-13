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

import java.io.Closeable
import java.sql.Connection
import java.sql.PreparedStatement
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import javax.sql.DataSource
import kotlin.concurrent.thread

/**
 * The experiment writer is a separate thread that is responsible for writing the results to the
 * database.
 */
public abstract class PostgresMetricsWriter<T>(
    private val ds: DataSource,
    private val batchSize: Int = 4096
) : Runnable, Closeable {
    /**
     * The queue of commands to process.
     */
    private val queue: BlockingQueue<Action> = ArrayBlockingQueue(12 * batchSize)

    /**
     * The thread for the actual writer.
     */
    private val writerThread: Thread = thread(name = "host-metrics-writer") { run() }

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
        queue.put(Action.Stop)
        writerThread.join()
    }

    /**
     * Create a prepared statement to use.
     */
    public abstract fun createStatement(conn: Connection): PreparedStatement

    /**
     * Persist the specified metrics using the given [stmt].
     */
    public abstract fun persist(action: Action.Write<T>, stmt: PreparedStatement)

    /**
     * Start the writer thread.
     */
    override fun run() {
        val conn = ds.connection
        var batch = 0

        conn.autoCommit = false
        val stmt = createStatement(conn)

        try {
            val actions = mutableListOf<Action>()

            loop@ while (true) {
                actions.clear()

                if (queue.isEmpty()) {
                    actions.add(queue.take())
                }
                queue.drainTo(actions)

                for (action in actions) {
                    when (action) {
                        is Action.Stop -> break@loop
                        is Action.Write<*> -> {
                            @Suppress("UNCHECKED_CAST")
                            persist(action as Action.Write<T>, stmt)
                            stmt.addBatch()
                            batch++

                            if (batch % batchSize == 0) {
                                stmt.executeBatch()
                                conn.commit()
                            }

                        }
                    }
                }
            }
        } finally {
            conn.commit()
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
