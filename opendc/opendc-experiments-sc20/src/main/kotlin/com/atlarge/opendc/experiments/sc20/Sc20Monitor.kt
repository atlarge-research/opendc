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
    private val lastServerStates = mutableMapOf<Server, Pair<ServerState, Long>>()

    init {
        outputFile.write("time,duration,requestedBurst,grantedBurst,overcommissionedBurst,interferedBurst,cpuUsage,cpuDemand,numberOfDeployedImages,server,hostState,hostUsage,powerDraw\n")
    }

    suspend fun onVmStateChanged(server: Server) {}


    suspend fun serverStateChanged(driver: VirtDriver, server: Server) {
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
        duration: Long = 5 * 60 * 1000L
    ) {
        // Assume for now that the host is not virtualized and measure the current power draw
        val driver = hostServer.services[BareMetalDriver.Key]
        val usage = driver.usage.first()
        val powerDraw = driver.powerDraw.first()

        outputFile.write("$time,$duration,$requestedBurst,$grantedBurst,$overcommissionedBurst,$interferedBurst,$cpuUsage,$cpuDemand,$numberOfDeployedImages,${hostServer.uid},${hostServer.state},$usage,$powerDraw")
        outputFile.newLine()
    }

    override fun close() {
        outputFile.flush()
        outputFile.close()
    }
}
