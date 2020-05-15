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

package com.atlarge.opendc.experiments.sc20.telemetry.parquet

import com.atlarge.opendc.experiments.sc20.telemetry.Event
import mu.KotlinLogging
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import java.io.Closeable
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import kotlin.concurrent.thread

/**
 * The logging instance to use.
 */
private val logger = KotlinLogging.logger {}

/**
 * A writer that writes events in Parquet format.
 */
public open class ParquetEventWriter<in T : Event>(
    private val path: File,
    private val schema: Schema,
    private val converter: (T, GenericData.Record) -> Unit,
    private val bufferSize: Int = 4096
) : Runnable, Closeable {
    /**
     * The queue of commands to process.
     */
    private val queue: BlockingQueue<Action> = ArrayBlockingQueue(bufferSize)

    /**
     * The thread that is responsible for writing the Parquet records.
     */
    private val writerThread = thread(start = true, name = "parquet-writer") { run() }

    /**
     * Write the specified metrics to the database.
     */
    public fun write(event: T) {
        queue.put(Action.Write(event))
    }

    /**
     * Signal the writer to stop.
     */
    public override fun close() {
        queue.put(Action.Stop)
        writerThread.join()
    }

    /**
     * Start the writer thread.
     */
    override fun run() {
        val writer = AvroParquetWriter.builder<GenericData.Record>(Path(path.absolutePath))
            .withSchema(schema)
            .withCompressionCodec(CompressionCodecName.SNAPPY)
            .withPageSize(4 * 1024 * 1024) // For compression
            .withRowGroupSize(16 * 1024 * 1024) // For write buffering (Page size)
            .build()

        try {
            loop@ while (true) {
                val action = queue.take()
                when (action) {
                    is Action.Stop -> break@loop
                    is Action.Write<*> -> {
                        val record = GenericData.Record(schema)
                        @Suppress("UNCHECKED_CAST")
                        converter(action.event as T, record)
                        writer.write(record)
                    }
                }
            }
        } catch (e: Throwable) {
            logger.error("Writer failed", e)
        } finally {
            writer.close()
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
        data class Write<out T : Event>(val event: T) : Action()
    }
}
