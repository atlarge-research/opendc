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

import com.atlarge.odcsim.simulationContext
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.metal.driver.BareMetalDriver
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import kotlinx.coroutines.flow.first
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

class ExperimentPostgresReporter(val scenario: Long, val run: Int, val writer: PostgresHostMetricsWriter) : ExperimentReporter {
    private val lastServerStates = mutableMapOf<Server, Pair<ServerState, Long>>()

    override suspend fun reportVmStateChange(server: Server) {}

    override suspend fun reportHostStateChange(
        driver: VirtDriver,
        server: Server,
        submittedVms: Long,
        queuedVms: Long,
        runningVms: Long,
        finishedVms: Long
    ) {
        val lastServerState = lastServerStates[server]
        if (server.state == ServerState.SHUTOFF && lastServerState != null) {
            val duration = simulationContext.clock.millis() - lastServerState.second
            reportHostSlice(
                simulationContext.clock.millis(),
                0,
                0,
                0,
                0,
                0.0,
                0.0,
                0,
                server,
                submittedVms,
                queuedVms,
                runningVms,
                finishedVms,
                duration
            )
        }

        logger.debug("Host ${server.uid} changed state ${server.state} [${simulationContext.clock.millis()}]")

        lastServerStates[server] = Pair(server.state, simulationContext.clock.millis())
    }

    override suspend fun reportHostSlice(
        time: Long,
        requestedBurst: Long,
        grantedBurst: Long,
        overcommissionedBurst: Long,
        interferedBurst: Long,
        cpuUsage: Double,
        cpuDemand: Double,
        numberOfDeployedImages: Int,
        hostServer: Server,
        submittedVms: Long,
        queuedVms: Long,
        runningVms: Long,
        finishedVms: Long,
        duration: Long
    ) {
        // Assume for now that the host is not virtualized and measure the current power draw
        val driver = hostServer.services[BareMetalDriver.Key]
        val powerDraw = driver.powerDraw.first()

        writer.write(
            scenario, run, HostMetrics(
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
                powerDraw
            )
        )
    }

    override fun close() {}
}
