package com.atlarge.opendc.experiments.sc20

import com.atlarge.odcsim.simulationContext
import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.core.ServerState
import com.atlarge.opendc.compute.metal.driver.BareMetalDriver
import com.atlarge.opendc.compute.virt.driver.VirtDriver
import kotlinx.coroutines.flow.first
import java.io.BufferedWriter
import java.io.Closeable
import java.io.FileWriter

class Sc20Monitor(
    destination: String
) : Closeable {
    private val outputFile = BufferedWriter(FileWriter(destination))
    private var failedInSlice: Int = 0
    private val lastServerStates = mutableMapOf<Server, Pair<ServerState, Long>>()

    init {
        outputFile.write("time,requestedBurst,grantedBurst,numberOfDeployedImages,server,hostUsage,powerDraw,failedVms\n")
    }

    suspend fun onVmStateChanged(server: Server) {
        println("[${simulationContext.clock.millis()}] ${server.uid} ${server.state}")

        if (server.state == ServerState.ERROR) {
            failedInSlice++
        }
    }

    suspend fun serverStateChanged(driver: VirtDriver, server: Server) {
        if ((server.state == ServerState.SHUTOFF || server.state == ServerState.ERROR) &&
            lastServerStates.containsKey(server)
        ) {
            val duration = simulationContext.clock.millis() - lastServerStates[server]!!.second
            onSliceFinish(
                simulationContext.clock.millis(),
                0,
                0,
                0,
                server,
                duration
            )
        }

        lastServerStates[server] = Pair(server.state, simulationContext.clock.millis())
    }

    suspend fun onSliceFinish(
        time: Long,
        requestedBurst: Long,
        grantedBurst: Long,
        numberOfDeployedImages: Int,
        hostServer: Server,
        duration: Long = 5 * 60 * 1000L
    ) {
        lastServerStates.remove(hostServer)

        // Assume for now that the host is not virtualized and measure the current power draw
        val driver = hostServer.services[BareMetalDriver.Key]
        val usage = driver.usage.first()
        val powerDraw = driver.powerDraw.first()
        val failed = if (failedInSlice > 0) {
            failedInSlice.also { failedInSlice = 0 }
        } else {
            0
        }
        outputFile.write("$time,$duration,$requestedBurst,$grantedBurst,$numberOfDeployedImages,${hostServer.uid},$usage,$powerDraw,$failed")
        outputFile.newLine()
    }

    override fun close() {
        outputFile.flush()
        outputFile.close()
    }
}
