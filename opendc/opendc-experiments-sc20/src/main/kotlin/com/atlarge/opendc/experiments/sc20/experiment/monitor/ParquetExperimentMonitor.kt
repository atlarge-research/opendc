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

package com.atlarge.opendc.experiments.sc20.experiment.monitor

import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import com.atlarge.opendc.compute.virt.service.VirtProvisioningEvent
import com.atlarge.opendc.experiments.sc20.experiment.Run
import com.atlarge.opendc.experiments.sc20.telemetry.HostEvent
import com.atlarge.opendc.experiments.sc20.telemetry.ProvisionerEvent
import com.atlarge.opendc.experiments.sc20.telemetry.parquet.ParquetHostEventWriter
import com.atlarge.opendc.experiments.sc20.telemetry.parquet.ParquetProvisionerEventWriter
import mu.KotlinLogging
import java.io.File

/**
 * The logger instance to use.
 */
private val logger = KotlinLogging.logger {}

/**
 * An [ExperimentMonitor] that logs the events to a Parquet file.
 */
class ParquetExperimentMonitor(val run: Run) : ExperimentMonitor {
    private val partition = "portfolio_id=${run.parent.parent.id}/scenario_id=${run.parent.id}/run_id=${run.id}"
    private val hostWriter = ParquetHostEventWriter(
        File(run.parent.parent.parent.output, "host-metrics/$partition/data.parquet"),
        run.parent.parent.parent.bufferSize
    )
    private val provisionerWriter = ParquetProvisionerEventWriter(
        File(run.parent.parent.parent.output, "provisioner-metrics/$partition/data.parquet"),
        run.parent.parent.parent.bufferSize
    )
    private val lastServerStates = mutableMapOf<Server, Pair<ServerState, Long>>()

    override fun reportVmStateChange(time: Long, server: Server) {}

    override fun reportHostStateChange(
        time: Long,
        driver: VirtDriver,
        server: Server
    ) {
        logger.debug("Host ${server.uid} changed state ${server.state} [$time]")

        val lastServerState = lastServerStates[server]
        if (server.state == ServerState.SHUTOFF && lastServerState != null) {
            val duration = time - lastServerState.second
            reportHostSlice(
                time,
                0,
                0,
                0,
                0,
                0.0,
                0.0,
                0,
                server,
                duration
            )

            lastServerStates.remove(server)
            lastPowerConsumption.remove(server)
        } else {
            lastServerStates[server] = Pair(server.state, time)
        }
    }

    private val lastPowerConsumption = mutableMapOf<Server, Double>()

    override fun reportPowerConsumption(host: Server, draw: Double) {
        lastPowerConsumption[host] = draw
    }

    override fun reportHostSlice(
        time: Long,
        requestedBurst: Long,
        grantedBurst: Long,
        overcommissionedBurst: Long,
        interferedBurst: Long,
        cpuUsage: Double,
        cpuDemand: Double,
        numberOfDeployedImages: Int,
        hostServer: Server,
        duration: Long
    ) {
        hostWriter.write(
            HostEvent(
                time,
                duration,
                hostServer,
                numberOfDeployedImages,
                requestedBurst,
                grantedBurst,
                overcommissionedBurst,
                interferedBurst,
                cpuUsage,
                cpuDemand,
                lastPowerConsumption[hostServer] ?: 200.0
            )
        )
    }

    override fun reportProvisionerMetrics(time: Long, event: VirtProvisioningEvent.MetricsAvailable) {
        provisionerWriter.write(
            ProvisionerEvent(
                time,
                event.totalHostCount,
                event.availableHostCount,
                event.totalVmCount,
                event.activeVmCount,
                event.inactiveVmCount,
                event.waitingVmCount,
                event.failedVmCount
            )
        )
    }

    override fun close() {
        hostWriter.close()
        provisionerWriter.close()
    }
}
