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

package org.opendc.experiments.compute.export.parquet

import org.opendc.experiments.compute.telemetry.ComputeMonitor
import org.opendc.experiments.compute.telemetry.table.HostTableReader
import org.opendc.experiments.compute.telemetry.table.ServerTableReader
import org.opendc.experiments.compute.telemetry.table.ServiceTableReader
import java.io.File

/**
 * A [ComputeMonitor] that logs the events to a Parquet file.
 */
public class ParquetComputeMonitor(base: File, partition: String, bufferSize: Int) : ComputeMonitor, AutoCloseable {
    private val serverWriter = ParquetServerDataWriter(
        File(base, "server/$partition/data.parquet").also { it.parentFile.mkdirs() },
        bufferSize
    )

    private val hostWriter = ParquetHostDataWriter(
        File(base, "host/$partition/data.parquet").also { it.parentFile.mkdirs() },
        bufferSize
    )

    private val serviceWriter = ParquetServiceDataWriter(
        File(base, "service/$partition/data.parquet").also { it.parentFile.mkdirs() },
        bufferSize
    )

    override fun record(reader: ServerTableReader) {
        serverWriter.write(reader)
    }

    override fun record(reader: HostTableReader) {
        hostWriter.write(reader)
    }

    override fun record(reader: ServiceTableReader) {
        serviceWriter.write(reader)
    }

    override fun close() {
        hostWriter.close()
        serviceWriter.close()
        serverWriter.close()
    }
}
