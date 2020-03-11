package com.atlarge.opendc.experiments.sc20

import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.metal.driver.BareMetalDriver
import com.atlarge.opendc.compute.virt.monitor.HypervisorMonitor
import kotlinx.coroutines.flow.first
import java.io.BufferedWriter
import java.io.Closeable
import java.io.FileWriter

class Sc20HypervisorMonitor(
    destination: String
) : HypervisorMonitor, Closeable {
    private val outputFile = BufferedWriter(FileWriter(destination))

    init {
        outputFile.write("time,requestedBurst,grantedBurst,numberOfDeployedImages,server,hostUsage,powerDraw\n")
    }

    override suspend fun onSliceFinish(
        time: Long,
        requestedBurst: Long,
        grantedBurst: Long,
        numberOfDeployedImages: Int,
        hostServer: Server
    ) {
        // Assume for now that the host is not virtualized and measure the current power draw
        val driver = hostServer.serviceRegistry[BareMetalDriver.Key]
        val usage = driver.usage.first()
        val powerDraw = driver.powerDraw.first()

        outputFile.write("$time,$requestedBurst,$grantedBurst,$numberOfDeployedImages,$numberOfDeployedImages,${hostServer.uid},$usage,$powerDraw")
        outputFile.newLine()
    }

    override fun close() {
        outputFile.flush()
        outputFile.close()
    }
}
