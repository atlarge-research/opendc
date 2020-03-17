package com.atlarge.opendc.experiments.sc20

import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.core.monitor.ServerMonitor
import com.atlarge.opendc.compute.metal.driver.BareMetalDriver
import com.atlarge.opendc.compute.virt.monitor.HypervisorMonitor
import kotlinx.coroutines.flow.first
import java.io.BufferedWriter
import java.io.Closeable
import java.io.FileWriter

class Sc20Monitor(
    destination: String
) : HypervisorMonitor, ServerMonitor, Closeable {
    private val outputFile = BufferedWriter(FileWriter(destination))
    private var failed: Int = 0

    init {
        outputFile.write("time,requestedBurst,grantedBurst,numberOfDeployedImages,server,hostUsage,powerDraw,failedVms\n")
    }

    override fun stateChanged(server: Server, previousState: ServerState) {
        println("${server.uid} ${server.state}")
        if (server.state == ServerState.ERROR) {
            failed++
        }
    }

    override suspend fun onSliceFinish(
        time: Long,
        requestedBurst: Long,
        grantedBurst: Long,
        numberOfDeployedImages: Int,
        hostServer: Server
    ) {
        // Assume for now that the host is not virtualized and measure the current power draw
        val driver = hostServer.services[BareMetalDriver.Key]
        val usage = driver.usage.first()
        val powerDraw = driver.powerDraw.first()

        outputFile.write("$time,$requestedBurst,$grantedBurst,$numberOfDeployedImages,${hostServer.uid},$usage,$powerDraw,$failed")
        outputFile.newLine()
    }

    override fun close() {
        outputFile.flush()
        outputFile.close()
    }
}
