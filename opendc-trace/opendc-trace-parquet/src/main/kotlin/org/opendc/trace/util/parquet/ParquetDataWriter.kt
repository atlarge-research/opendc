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
import org.apache.parquet.hadoop.ParquetFileReader
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
 * The number of records handed to a writer thread at once, so that the queue's lock is taken once per batch rather
 * than once per record.
 */
private const val BATCH_SIZE = 256

/**
 * A writer that writes data in Parquet format.
 *
 * The records are written on separate threads, to which they are handed in batches. With more than one writer thread,
 * each thread writes the batches it is handed to a part file of its own in a hidden directory next to the output file,
 * and the part files are merged into the output file when the writer is closed, so the rows of one part precede those
 * of the next. The records are routed to the threads by their [shardKey], so that the records with the same key are
 * written in order, or without one, the batches are handed to the threads in turn. [write] and [close] must be called
 * from a single thread, and [close] throws if a writer thread failed.
 *
 * @param path The path to the file to write the data to.
 * @param writeSupport Creates the [WriteSupport] that converts the records to Parquet format, once per writer thread.
 * @param bufferSize The maximum number of records waiting to be written.
 * @param writerThreads The number of threads that write the records.
 * @param shardKey The key of a record that determines its writer thread, or `null` to hand the batches to the threads
 * in turn.
 */
public abstract class ParquetDataWriter<in T>(
    private val path: File,
    private val writeSupport: () -> WriteSupport<T>,
    bufferSize: Int = 4096,
    writerThreads: Int = 1,
    private val shardKey: ((T) -> Int)? = null,
) : AutoCloseable {
    init {
        require(bufferSize > 0) { "Buffer size must be positive" }
        require(writerThreads > 0) { "The number of writer threads must be positive" }
    }

    /**
     * The logging instance to use.
     */
    private val logger = KotlinLogging.logger {}

    /**
     * The number of records in a full batch.
     */
    private val batchSize = min(BATCH_SIZE, bufferSize)

    /**
     * The first failure of a writer thread, to be propagated to the actual writer.
     */
    @Volatile
    private var exception: Throwable? = null

    /**
     * A flag to indicate that the writer has been closed.
     */
    private var isClosed = false

    /**
     * The files written by the writer threads: the output file for a single thread, and otherwise a part file per thread.
     */
    private val files = if (writerThreads == 1) listOf(path) else createPartFiles(writerThreads)

    /**
     * The writer threads, one per file, which share the buffer.
     */
    private val shards = files.map { Shard(it, max(1, bufferSize / batchSize / writerThreads)) }

    /**
     * Whether the records are routed to the writer threads by their [shardKey].
     */
    private val isRoutedByKey = shardKey != null && shards.size > 1

    /**
     * The records written since the last batch was handed to a writer thread: a batch per thread when the records are
     * routed by their key, and otherwise a single batch.
     */
    private val batches = Array(if (isRoutedByKey) shards.size else 1) { ArrayList<Any?>(batchSize) }

    /**
     * The index of the writer thread the next batch is handed to, when the records are not routed by their key.
     */
    private var nextShard = 0

    /**
     * Build the [ParquetWriter] used to write the Parquet files.
     */
    protected open fun buildWriter(builder: LocalParquetWriter.Builder<@UnsafeVariance T>): ParquetWriter<@UnsafeVariance T> {
        return builder.build()
    }

    /**
     * Write the specified metrics to the database.
     *
     * @throws IllegalStateException if the writer is closed or a writer thread failed.
     */
    public fun write(data: T) {
        check(!isClosed) { "Writer is closed" }

        val index = if (isRoutedByKey) Math.floorMod(shardKey!!(data), shards.size) else 0
        val batch = batches[index]
        batch.add(data)
        if (exception != null || (batch.size >= batchSize && !offerBatch(index))) {
            throw IllegalStateException("Writer thread failed", exception)
        }
    }

    /**
     * Signal the writer to stop, and wait for the writer threads to finish.
     *
     * @throws IllegalStateException if a writer thread failed, in which case the output file is incomplete or missing.
     */
    override fun close() {
        if (isClosed) {
            return
        }

        isClosed = true

        for (index in batches.indices) {
            if (batches[index].isNotEmpty()) {
                offerBatch(index)
            }
        }

        for (shard in shards) {
            shard.finish()
        }

        if (shards.size > 1) {
            mergeParts()
        }

        exception?.let { throw IllegalStateException("Writer thread failed", it) }
    }

    /**
     * Create the part files of [count] writer threads, in a hidden directory next to the output file that is emptied
     * first, as a run that was aborted may have left it behind.
     */
    private fun createPartFiles(count: Int): List<File> {
        val directory = path.resolveSibling(".${path.name}.parts")
        directory.deleteRecursively()
        directory.mkdirs()
        return List(count) { File(directory, "part-$it.parquet") }
    }

    /**
     * Hand the batch at [index] to its writer thread, and start a new one.
     *
     * @return `false` if the writer thread stopped before the batch could be handed over.
     */
    private fun offerBatch(index: Int): Boolean {
        val full = batches[index]
        batches[index] = ArrayList(batchSize)

        val shard =
            if (isRoutedByKey) {
                shards[index]
            } else {
                shards[nextShard].also { nextShard = (nextShard + 1) % shards.size }
            }
        return shard.offer(full)
    }

    /**
     * Merge the part files into the output file, by copying their row groups without decoding them, and delete them.
     */
    private fun mergeParts() {
        try {
            // A failed writer thread leaves an incomplete part, so its failure has been reported instead
            if (exception != null) {
                return
            }

            val metadata = ParquetFileReader.open(LocalInputFile(files.first())).use { it.footer.fileMetaData }
            val writer =
                ParquetFileWriter(
                    LocalOutputFile(path),
                    metadata.schema,
                    ParquetFileWriter.Mode.OVERWRITE,
                    ParquetWriter.DEFAULT_BLOCK_SIZE.toLong(),
                    0,
                )
            writer.start()
            for (file in files) {
                writer.appendFile(LocalInputFile(file))
            }
            writer.end(metadata.keyValueMetaData)
        } catch (e: Throwable) {
            logger.error(e) { "Failure in merging the parts of $path" }
            exception = e
        } finally {
            files.first().parentFile.deleteRecursively()
        }
    }

    /**
     * A writer thread, which writes the batches it is handed to [file].
     *
     * @param capacity The number of batches that can wait to be written.
     */
    private inner class Shard(private val file: File, capacity: Int) {
        /**
         * The queue of batches to write, terminated by [EndOfRecords] when the writer is closed.
         */
        private val queue: BlockingQueue<Any> = ArrayBlockingQueue(capacity)

        /**
         * The thread that is responsible for writing the Parquet records.
         */
        private val writerThread =
            // A daemon thread, so that a run that is aborted before the writer is closed does not keep the process alive
            thread(isDaemon = true, name = "${this@ParquetDataWriter}-${file.name}") {
                try {
                    val builder =
                        LocalParquetWriter.builder(file.toPath(), writeSupport())
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
         * Add [item] to the queue, waiting for space while the writer thread is running.
         *
         * @return `false` if the writer thread stopped before [item] could be added.
         */
        fun offer(item: Any): Boolean {
            // Wait in bounded steps, as a writer thread that stops while the queue is full would otherwise block forever
            while (writerThread.isAlive) {
                if (queue.offer(item, 100, TimeUnit.MILLISECONDS)) {
                    return true
                }
            }

            return false
        }

        /**
         * Let the writer thread write the batches it was handed and close its file, and wait for it. A writer thread
         * that failed has already closed its file.
         */
        fun finish() {
            offer(EndOfRecords)
            writerThread.join()
        }
    }

    /**
     * Marks the end of the records in the queue.
     */
    private object EndOfRecords
}
