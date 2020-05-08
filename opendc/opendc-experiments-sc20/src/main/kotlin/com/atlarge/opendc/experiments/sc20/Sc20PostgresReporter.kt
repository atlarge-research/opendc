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

package com.atlarge.opendc.experiments.sc20

import com.atlarge.odcsim.simulationContext
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.metal.driver.BareMetalDriver
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import kotlinx.coroutines.flow.first
import mu.KotlinLogging
import java.sql.Connection
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

class Sc20PostgresReporter(val conn: Connection, val experimentId: Long) : Sc20Reporter {
    private val lastServerStates = mutableMapOf<Server, Pair<ServerState, Long>>()
    private val queue = ArrayBlockingQueue<Report>(2048)
    private val stop = AtomicBoolean(false)
    private val writerThread = thread(start = true, name = "sc20-writer") {
        val stmt = try {
            conn.prepareStatement(
                """
                    INSERT INTO host_reports (experiment_id, time, duration, requested_burst, granted_burst, overcommissioned_burst, interfered_burst, cpu_usage, cpu_demand, image_count, server, host_state, host_usage, power_draw, total_submitted_vms, total_queued_vms, total_running_vms, total_finished_vms)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent()
            )
        } catch (e: Throwable) {
            conn.close()
            throw e
        }

        val batchSize = 4096
        var batch = 0

        try {
            while (!stop.get()) {
                val record = queue.take()
                stmt.setLong(1, experimentId)
                stmt.setLong(2, record.time)
                stmt.setLong(3, record.duration)
                stmt.setLong(4, record.requestedBurst)
                stmt.setLong(5, record.grantedBurst)
                stmt.setLong(6, record.overcommissionedBurst)
                stmt.setLong(7, record.interferedBurst)
                stmt.setDouble(8, record.cpuUsage)
                stmt.setDouble(9, record.cpuDemand)
                stmt.setInt(10, record.numberOfDeployedImages)
                stmt.setString(11, record.hostServer.uid.toString())
                stmt.setString(12, record.hostServer.state.name)
                stmt.setDouble(13, record.hostUsage)
                stmt.setDouble(14, record.powerDraw)
                stmt.setLong(15, record.submittedVms)
                stmt.setLong(16, record.queuedVms)
                stmt.setLong(17, record.runningVms)
                stmt.setLong(18, record.finishedVms)
                stmt.addBatch()
                batch++

                if (batch > batchSize) {
                    stmt.executeBatch()
                    batch = 0
                }
            }
        } finally {
            stmt.executeBatch()
            stmt.close()
            conn.close()
        }
    }

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

        logger.info("Host ${server.uid} changed state ${server.state} [${simulationContext.clock.millis()}]")

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
        val usage = driver.usage.first()
        val powerDraw = driver.powerDraw.first()

        queue.put(
            Report(
                time,
                duration,
                requestedBurst,
                grantedBurst,
                overcommissionedBurst,
                interferedBurst,
                cpuUsage,
                cpuDemand,
                numberOfDeployedImages,
                hostServer,
                usage,
                powerDraw,
                submittedVms,
                queuedVms,
                runningVms,
                finishedVms
            )
        )
    }

    override fun close() {
        // Busy loop to wait for writer thread to finish
        stop.set(true)
        writerThread.join()
    }

    data class Report(
        val time: Long,
        val duration: Long,
        val requestedBurst: Long,
        val grantedBurst: Long,
        val overcommissionedBurst: Long,
        val interferedBurst: Long,
        val cpuUsage: Double,
        val cpuDemand: Double,
        val numberOfDeployedImages: Int,
        val hostServer: Server,
        val hostUsage: Double,
        val powerDraw: Double,
        val submittedVms: Long,
        val queuedVms: Long,
        val runningVms: Long,
        val finishedVms: Long
    )
}
