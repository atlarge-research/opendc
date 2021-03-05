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
import org.opendc.compute.core.Server
import org.opendc.compute.core.metal.Node
import org.opendc.compute.core.virt.Host
import org.opendc.compute.core.virt.service.VirtProvisioningEvent
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
        File(base, "host-metrics/$partition/data.parquet"),
        bufferSize
    )
    private val provisionerWriter = ParquetProvisionerEventWriter(
        File(base, "provisioner-metrics/$partition/data.parquet"),
        bufferSize
    )
    private val currentHostEvent = mutableMapOf<Node, HostEvent>()
    private var startTime = -1L

    override fun reportVmStateChange(time: Long, server: Server) {
        if (startTime < 0) {
            startTime = time

            // Update timestamp of initial event
            currentHostEvent.replaceAll { _, v -> v.copy(timestamp = startTime) }
        }
    }

    override fun reportHostStateChange(
        time: Long,
        driver: Host,
        host: Node
    ) {
        logger.debug { "Host ${host.uid} changed state ${host.state} [$time]" }

        val previousEvent = currentHostEvent[host]

        val roundedTime = previousEvent?.let {
            val duration = time - it.timestamp
            val k = 5 * 60 * 1000L // 5 min in ms
            val rem = duration % k

            if (rem == 0L) {
                time
            } else {
                it.timestamp + duration + k - rem
            }
        } ?: time

        reportHostSlice(
            roundedTime,
            0,
            0,
            0,
            0,
            0.0,
            0.0,
            0,
            host
        )
    }

    private val lastPowerConsumption = mutableMapOf<Node, Double>()

    override fun reportPowerConsumption(host: Node, draw: Double) {
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
        host: Node,
        duration: Long
    ) {
        val previousEvent = currentHostEvent[host]
        when {
            previousEvent == null -> {
                val event = HostEvent(
                    time,
                    5 * 60 * 1000L,
                    host,
                    numberOfDeployedImages,
                    requestedBurst,
                    grantedBurst,
                    overcommissionedBurst,
                    interferedBurst,
                    cpuUsage,
                    cpuDemand,
                    lastPowerConsumption[host] ?: 200.0,
                    host.flavor.cpuCount
                )

                currentHostEvent[host] = event
            }
            previousEvent.timestamp == time -> {
                val event = HostEvent(
                    time,
                    previousEvent.duration,
                    host,
                    numberOfDeployedImages,
                    requestedBurst,
                    grantedBurst,
                    overcommissionedBurst,
                    interferedBurst,
                    cpuUsage,
                    cpuDemand,
                    lastPowerConsumption[host] ?: 200.0,
                    host.flavor.cpuCount
                )

                currentHostEvent[host] = event
            }
            else -> {
                hostWriter.write(previousEvent)

                val event = HostEvent(
                    time,
                    time - previousEvent.timestamp,
                    host,
                    numberOfDeployedImages,
                    requestedBurst,
                    grantedBurst,
                    overcommissionedBurst,
                    interferedBurst,
                    cpuUsage,
                    cpuDemand,
                    lastPowerConsumption[host] ?: 200.0,
                    host.flavor.cpuCount
                )

                currentHostEvent[host] = event
            }
        }
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
        // Flush remaining events
        for ((_, event) in currentHostEvent) {
            hostWriter.write(event)
        }
        currentHostEvent.clear()

        hostWriter.close()
        provisionerWriter.close()
    }
}
