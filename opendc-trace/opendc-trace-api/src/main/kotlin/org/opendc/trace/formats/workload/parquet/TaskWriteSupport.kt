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

package org.opendc.trace.formats.workload.parquet

import org.apache.hadoop.conf.Configuration
import org.apache.parquet.hadoop.api.WriteSupport
import org.apache.parquet.io.api.Binary
import org.apache.parquet.io.api.RecordConsumer
import kotlin.math.roundToLong

/**
 * Support for writing [Task] instances to Parquet format.
 */
internal class TaskWriteSupport : WriteSupport<Task>() {
    /**
     * The current active record consumer.
     */
    private lateinit var recordConsumer: RecordConsumer

    override fun init(configuration: Configuration): WriteContext {
        return WriteContext(TASK_SCHEMA, emptyMap())
    }

    override fun prepareForWrite(recordConsumer: RecordConsumer) {
        this.recordConsumer = recordConsumer
    }

    override fun write(record: Task) {
        write(recordConsumer, record)
    }

    private fun write(
        consumer: RecordConsumer,
        record: Task,
    ) {
        consumer.startMessage()

        consumer.startField("id", 0)
        consumer.addInteger(record.id)
        consumer.endField("id", 0)

        consumer.startField("submission_time", 1)
        consumer.addLong(record.submissionTime.toEpochMilli())
        consumer.endField("submission_time", 1)

        consumer.startField("duration", 2)
        consumer.addLong(record.durationTime)
        consumer.endField("duration", 2)

        consumer.startField("cpu_count", 3)
        consumer.addInteger(record.cpuCount)
        consumer.endField("cpu_count", 3)

        consumer.startField("cpu_capacity", 4)
        consumer.addDouble(record.cpuCapacity)
        consumer.endField("cpu_capacity", 4)

        consumer.startField("mem_capacity", 5)
        consumer.addLong(record.memCapacity.roundToLong())
        consumer.endField("mem_capacity", 5)

        record.nature?.let {
            consumer.startField("nature", 6)
            consumer.addBinary(Binary.fromCharSequence(it))
            consumer.endField("nature", 6)
        }

        if (record.deadline != -1L) {
            consumer.startField("deadline", 7)
            consumer.addLong(record.deadline)
            consumer.endField("deadline", 7)
        }

        consumer.endMessage()
    }
}
