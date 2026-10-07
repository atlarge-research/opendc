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

package org.opendc.trace.parquet

import mu.KotlinLogging
import org.apache.parquet.column.ParquetProperties
import org.apache.parquet.hadoop.ParquetFileWriter
import org.apache.parquet.hadoop.ParquetWriter
import org.apache.parquet.hadoop.api.WriteSupport
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.BlockingQueue
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.math.max
import kotlin.math.min

/**
 * The number of records handed to the writer thread at once, so that the queue's lock is taken once per batch rather
 * than once per record.
 */
private const val BATCH_SIZE = 256

/**
 * A writer that writes data in Parquet format.
 *
 * The records are written on a separate thread, to which they are handed in batches. [write] and [close] must be called
 * from a single thread.
 *
 * @param path The path to the file to write the data to.
 * @param writeSupport The [WriteSupport] implementation for converting the records to Parquet format.
 * @param bufferSize The maximum number of records waiting to be written.
 */
public abstract class ParquetDataWriter<in T>(
    path: File,
    private val writeSupport: WriteSupport<T>,
    bufferSize: Int = 4096,
) : AutoCloseable {
    /**
     * The logging instance to use.
     */
    private val logger = KotlinLogging.logger {}

    init {
        require(bufferSize > 0) { "Buffer size must be positive" }
    }

    /**
     * The number of records in a full batch.
     */
    private val batchSize = min(BATCH_SIZE, bufferSize)

    /**
     * The queue of batches to write, terminated by [EndOfRecords] when the writer is closed.
     */
    private val queue: BlockingQueue<Any> = ArrayBlockingQueue(max(1, bufferSize / batchSize))

    /**
     * The records written since the last batch was handed to the writer thread.
     */
    private var batch = ArrayList<Any?>(batchSize)

    /**
     * An exception to be propagated to the actual writer.
     */
    @Volatile
    private var exception: Throwable? = null

    /**
     * A flag to indicate that the writer has been closed.
     */
    private var isClosed = false

    /**
     * The thread that is responsible for writing the Parquet records.
     */
    private val writerThread =
        thread(start = false, name = this.toString()) {
            try {
                val builder =
                    LocalParquetWriter.builder(path.toPath(), writeSupport)
                        .withWriterVersion(ParquetProperties.WriterVersion.PARQUET_2_0)
                        .withCompressionCodec(CompressionCodecName.ZSTD)
                        .withWriteMode(ParquetFileWriter.Mode.OVERWRITE)

                buildWriter(builder).use { writer ->
                    val queue = queue
                    val buf = mutableListOf<Any>()
                    var shouldStop = false

                    while (!shouldStop) {
                        buf.add(queue.take())
                        queue.drainTo(buf)

                        for (item in buf) {
                            if (item === EndOfRecords) {
                                shouldStop = true
                                break
                            }

                            for (data in item as List<*>) {
                                @Suppress("UNCHECKED_CAST")
                                writer.write(data as T)
                            }
                        }
                        buf.clear()
                    }
                }
            } catch (e: Throwable) {
                logger.error(e) { "Failure in Parquet data writer" }
                exception = e
            }
        }

    /**
     * Build the [ParquetWriter] used to write the Parquet files.
     */
    protected open fun buildWriter(builder: LocalParquetWriter.Builder<@UnsafeVariance T>): ParquetWriter<@UnsafeVariance T> {
        return builder.build()
    }

    /**
     * Write the specified metrics to the database.
     *
     * @throws IllegalStateException if the writer is closed or the writer thread failed.
     */
    public fun write(data: T) {
        check(!isClosed) { "Writer is closed" }

        batch.add(data)
        if (exception != null || (batch.size >= batchSize && !offerBatch())) {
            throw IllegalStateException("Writer thread failed", exception)
        }
    }

    /**
     * Signal the writer to stop.
     */
    override fun close() {
        if (isClosed) {
            return
        }

        isClosed = true

        // The writer thread closes the file once it reaches the marker, or has already closed it if it failed
        if (batch.isEmpty() || offerBatch()) {
            offer(EndOfRecords)
        }
        writerThread.join()
    }

    /**
     * Hand the current batch to the writer thread, and start a new one.
     *
     * @return `false` if the writer thread stopped before the batch could be handed over.
     */
    private fun offerBatch(): Boolean {
        val full = batch
        batch = ArrayList(batchSize)
        return offer(full)
    }

    /**
     * Add [item] to the queue, waiting for space while the writer thread is running.
     *
     * @return `false` if the writer thread stopped before [item] could be added.
     */
    private fun offer(item: Any): Boolean {
        // Wait in bounded steps, as a writer thread that stops while the queue is full would otherwise block forever
        while (writerThread.isAlive) {
            if (queue.offer(item, 100, TimeUnit.MILLISECONDS)) {
                return true
            }
        }

        return false
    }

    /**
     * Marks the end of the records in the queue.
     */
    private object EndOfRecords

    init {
        writerThread.start()
    }
}
