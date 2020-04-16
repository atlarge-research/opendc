package com.atlarge.opendc.experiments.sc20

import com.atlarge.odcsim.simulationContext
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.metal.driver.BareMetalDriver
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import kotlinx.coroutines.flow.first
import org.apache.avro.SchemaBuilder
import org.apache.avro.generic.GenericData
import org.apache.hadoop.fs.Path
import org.apache.parquet.avro.AvroParquetWriter
import org.apache.parquet.hadoop.metadata.CompressionCodecName
import java.io.Closeable
import java.util.concurrent.ArrayBlockingQueue
import kotlin.concurrent.thread

class Sc20Monitor(
    destination: String
) : Closeable {
    private val lastServerStates = mutableMapOf<Server, Pair<ServerState, Long>>()
    private val schema = SchemaBuilder
        .record("slice")
        .namespace("com.atlarge.opendc.experiments.sc20")
        .fields()
        .name("time").type().longType().noDefault()
        .name("duration").type().longType().noDefault()
        .name("requestedBurst").type().longType().noDefault()
        .name("grantedBurst").type().longType().noDefault()
        .name("overcommissionedBurst").type().longType().noDefault()
        .name("interferedBurst").type().longType().noDefault()
        .name("cpuUsage").type().doubleType().noDefault()
        .name("cpuDemand").type().doubleType().noDefault()
        .name("numberOfDeployedImages").type().intType().noDefault()
        .name("server").type().stringType().noDefault()
        .name("hostState").type().stringType().noDefault()
        .name("hostUsage").type().doubleType().noDefault()
        .name("powerDraw").type().doubleType().noDefault()
        .name("totalSubmittedVms").type().longType().noDefault()
        .name("totalQueuedVms").type().longType().noDefault()
        .name("totalRunningVms").type().longType().noDefault()
        .name("totalFinishedVms").type().longType().noDefault()
        .endRecord()
    private val writer = AvroParquetWriter.builder<GenericData.Record>(Path(destination))
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

    suspend fun onVmStateChanged(server: Server) {}

    suspend fun serverStateChanged(
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
            onSliceFinish(
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

        println("[${simulationContext.clock.millis()}] HOST ${server.uid} ${server.state}")

        lastServerStates[server] = Pair(server.state, simulationContext.clock.millis())
    }

    suspend fun onSliceFinish(
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
        duration: Long = 5 * 60 * 1000L
    ) {
        // Assume for now that the host is not virtualized and measure the current power draw
        val driver = hostServer.services[BareMetalDriver.Key]
        val usage = driver.usage.first()
        val powerDraw = driver.powerDraw.first()

        val record = GenericData.Record(schema)
        record.put("time", time)
        record.put("duration", duration)
        record.put("requestedBurst", requestedBurst)
        record.put("grantedBurst", grantedBurst)
        record.put("overcommissionedBurst", overcommissionedBurst)
        record.put("interferedBurst", interferedBurst)
        record.put("cpuUsage", cpuUsage)
        record.put("cpuDemand", cpuDemand)
        record.put("numberOfDeployedImages", numberOfDeployedImages)
        record.put("server", hostServer.uid)
        record.put("hostState", hostServer.state)
        record.put("hostUsage", usage)
        record.put("powerDraw", powerDraw)
        record.put("totalSubmittedVms", submittedVms)
        record.put("totalQueuedVms", queuedVms)
        record.put("totalRunningVms", runningVms)
        record.put("totalFinishedVms", finishedVms)

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
