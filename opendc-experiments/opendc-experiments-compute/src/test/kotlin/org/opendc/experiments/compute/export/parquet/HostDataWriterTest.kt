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

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.opendc.experiments.compute.telemetry.table.HostInfo
import org.opendc.experiments.compute.telemetry.table.HostTableReader
import java.nio.file.Files
import java.time.Instant

/**
 * Test suite for [ParquetHostDataWriter]
 */
class HostDataWriterTest {
    /**
     * The path to write the data file to.
     */
    private val path = Files.createTempFile("opendc", "parquet")

    /**
     * The writer used to write the data.
     */
    private val writer = ParquetHostDataWriter(path.toFile(), bufferSize = 4096)

    @AfterEach
    fun tearDown() {
        writer.close()
        Files.deleteIfExists(path)
    }

    @Test
    fun testSmoke() {
        assertDoesNotThrow {
            writer.write(object : HostTableReader {
                override val timestamp: Instant = Instant.now()
                override val host: HostInfo = HostInfo("id", "test", "x86", 4, 4096)
                override val guestsTerminated: Int = 0
                override val guestsRunning: Int = 0
                override val guestsError: Int = 0
                override val guestsInvalid: Int = 0
                override val cpuLimit: Double = 4096.0
                override val cpuUsage: Double = 1.0
                override val cpuDemand: Double = 1.0
                override val cpuUtilization: Double = 0.0
                override val cpuActiveTime: Long = 1
                override val cpuIdleTime: Long = 1
                override val cpuStealTime: Long = 1
                override val cpuLostTime: Long = 1
                override val powerUsage: Double = 1.0
                override val powerTotal: Double = 1.0
                override val uptime: Long = 1
                override val downtime: Long = 1
                override val bootTime: Instant? = null
            })
        }
    }
}
