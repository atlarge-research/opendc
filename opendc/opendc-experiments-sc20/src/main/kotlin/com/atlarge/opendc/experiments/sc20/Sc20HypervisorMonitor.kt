package com.atlarge.opendc.experiments.sc20

import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.virt.monitor.HypervisorMonitor
import java.io.BufferedWriter
import java.io.Closeable
import java.io.FileWriter

class Sc20HypervisorMonitor : HypervisorMonitor, Closeable {
    private val outputFile = BufferedWriter(FileWriter("sc20-experiment-results.csv"))

    init {
        outputFile.write("time,requestedBurst,grantedBurst,numberOfDeployedImages,server\n")
    }

    override fun onSliceFinish(
        time: Long,
        requestedBurst: Long,
        grantedBurst: Long,
        numberOfDeployedImages: Int,
        hostServer: Server
    ) {
        outputFile.write("$time,$requestedBurst,$grantedBurst,$numberOfDeployedImages,$numberOfDeployedImages,${hostServer.uid}\n")
    }

    override fun close() {
        outputFile.close()
    }
}
