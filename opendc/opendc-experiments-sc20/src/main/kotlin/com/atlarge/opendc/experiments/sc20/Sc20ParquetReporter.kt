package com.atlarge.opendc.experiments.sc20

import com.atlarge.odcsim.simulationContext
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.metal.driver.BareMetalDriver
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import kotlinx.coroutines.flow.first
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

class Sc20ParquetReporter(destination: File) : Sc20Reporter {
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
        record.put("host_usage", usage)
        record.put("power_draw", powerDraw)
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
