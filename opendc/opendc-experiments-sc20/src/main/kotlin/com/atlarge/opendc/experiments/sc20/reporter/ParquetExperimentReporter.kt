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
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import mu.KotlinLogging
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import java.io.File
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

private val logger = KotlinLogging.logger {}

class ExperimentParquetReporter(destination: File) :
    ExperimentReporter {
    private val lastServerStates = mutableMapOf<Server, Pair<ServerState, Long>>()
    private val schema = SchemaBuilder
        .record("slice")
        .namespace("com.atlarge.opendc.experiments.sc20")
        .fields()
        .name("time").type().longType().noDefault()
        .name("duration").type().longType().noDefault()
        .name("requested_burst").type().longType().noDefault()
        .name("granted_burst").type().longType().noDefault()
        .name("overcommissioned_burst").type().longType().noDefault()
        .name("interfered_burst").type().longType().noDefault()
        .name("cpu_usage").type().doubleType().noDefault()
        .name("cpu_demand").type().doubleType().noDefault()
        .name("image_count").type().intType().noDefault()
        .name("server").type().stringType().noDefault()
        .name("host_state").type().stringType().noDefault()
        .name("host_usage").type().doubleType().noDefault()
        .name("power_draw").type().doubleType().noDefault()
        .name("total_submitted_vms").type().longType().noDefault()
        .name("total_queued_vms").type().longType().noDefault()
        .name("total_running_vms").type().longType().noDefault()
        .name("total_finished_vms").type().longType().noDefault()
        .endRecord()
    private val writer = AvroParquetWriter.builder<GenericData.Record>(Path(destination.absolutePath))
        .withSchema(schema)
        .withCompressionCodec(CompressionCodecName.SNAPPY)
        .withPageSize(4 * 1024 * 1024) // For compression
        .withRowGroupSize(16 * 1024 * 1024) // For write buffering (Page size)
        .build()
    private val queue = ArrayBlockingQueue<GenericData.Record>(2048)
    private val writerThread = thread(start = true, name = "sc20-writer") {
        try {
            while (true) {
                val record = queue.take()
                writer.write(record)
            }
        } catch (e: InterruptedException) {
            // Do not rethrow this
        } finally {
            writer.close()
        }
    }

    override fun reportVmStateChange(time: Long, server: Server) {}

    override fun reportHostStateChange(
        time: Long,
        driver: VirtDriver,
        server: Server,
        submittedVms: Long,
        queuedVms: Long,
        runningVms: Long,
        finishedVms: Long
    ) {
        logger.info("Host ${server.uid} changed state ${server.state} [$time]")

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
                submittedVms,
                queuedVms,
                runningVms,
                finishedVms,
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
        submittedVms: Long,
        queuedVms: Long,
        runningVms: Long,
        finishedVms: Long,
        duration: Long
    ) {
        val record = GenericData.Record(schema)
        record.put("time", time)
        record.put("duration", duration)
        record.put("requested_burst", requestedBurst)
        record.put("granted_burst", grantedBurst)
        record.put("overcommissioned_burst", overcommissionedBurst)
        record.put("interfered_burst", interferedBurst)
        record.put("cpu_usage", cpuUsage)
        record.put("cpu_demand", cpuDemand)
        record.put("image_count", numberOfDeployedImages)
        record.put("server", hostServer.uid)
        record.put("host_state", hostServer.state)
        record.put("host_usage", cpuUsage)
        record.put("power_draw", lastPowerConsumption[hostServer] ?: 200.0)
        record.put("total_submitted_vms", submittedVms)
        record.put("total_queued_vms", queuedVms)
        record.put("total_running_vms", runningVms)
        record.put("total_finished_vms", finishedVms)

        queue.put(record)
    }

    override fun close() {
        // Busy loop to wait for writer thread to finish
        while (queue.isNotEmpty()) {
            Thread.sleep(500)
        }
        writerThread.interrupt()
    }
}
