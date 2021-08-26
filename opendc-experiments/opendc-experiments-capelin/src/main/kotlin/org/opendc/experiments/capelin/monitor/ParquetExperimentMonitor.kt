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

package org.opendc.experiments.capelin.monitor

import mu.KotlinLogging
import org.opendc.compute.api.Server
import org.opendc.compute.api.ServerState
import org.opendc.compute.service.driver.Host
import org.opendc.compute.service.driver.HostState
import org.opendc.experiments.capelin.telemetry.HostEvent
import org.opendc.experiments.capelin.telemetry.ProvisionerEvent
import org.opendc.experiments.capelin.telemetry.parquet.ParquetHostEventWriter
import org.opendc.experiments.capelin.telemetry.parquet.ParquetProvisionerEventWriter
import java.io.File

/**
 * The logger instance to use.
 */
private val logger = KotlinLogging.logger {}

/**
 * An [ExperimentMonitor] that logs the events to a Parquet file.
 */
public class ParquetExperimentMonitor(base: File, partition: String, bufferSize: Int) : ExperimentMonitor {
    private val hostWriter = ParquetHostEventWriter(
        File(base, "host-metrics/$partition/data.parquet").also { it.parentFile.mkdirs() },
        bufferSize
    )
    private val provisionerWriter = ParquetProvisionerEventWriter(
        File(base, "provisioner-metrics/$partition/data.parquet").also { it.parentFile.mkdirs() },
        bufferSize
    )

    override fun reportVmStateChange(time: Long, server: Server, newState: ServerState) {}

    override fun reportHostStateChange(time: Long, host: Host, newState: HostState) {
        logger.debug { "Host ${host.uid} changed state $newState [$time]" }
    }

    override fun reportHostData(
        time: Long,
        totalWork: Double,
        grantedWork: Double,
        overcommittedWork: Double,
        interferedWork: Double,
        cpuUsage: Double,
        cpuDemand: Double,
        powerDraw: Double,
        instanceCount: Int,
        uptime: Long,
        downtime: Long,
        host: Host
    ) {
        hostWriter.write(
            HostEvent(
                time,
                5 * 60 * 1000L,
                host,
                instanceCount,
                totalWork.toLong(),
                grantedWork.toLong(),
                overcommittedWork.toLong(),
                interferedWork.toLong(),
                cpuUsage,
                cpuDemand,
                powerDraw,
                host.model.cpuCount
            )
        )
    }

    override fun reportServiceData(
        time: Long,
        totalHostCount: Int,
        availableHostCount: Int,
        totalVmCount: Int,
        activeVmCount: Int,
        inactiveVmCount: Int,
        waitingVmCount: Int,
        failedVmCount: Int
    ) {
        provisionerWriter.write(
            ProvisionerEvent(
                time,
                totalHostCount,
                availableHostCount,
                totalVmCount,
                activeVmCount,
                inactiveVmCount,
                waitingVmCount,
                failedVmCount
            )
        )
    }

    override fun close() {
        hostWriter.close()
        provisionerWriter.close()
    }
}
