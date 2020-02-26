package com.atlarge.opendc.experiments.sc20

import com.atlarge.opendc.compute.core.Server
import com.atlarge.opendc.compute.virt.monitor.HypervisorMonitor
import java.io.File

class Sc20HypervisorMonitor : HypervisorMonitor {
    private val outputFile = File("sc20-experiment-results.csv")

    init {
        outputFile.writeText("time,requestedBurst,grantedBurst,numberOfDeployedImages,server\n")
    }

    override fun onSliceFinish(
        time: Long,
        requestedBurst: Long,
        grantedBurst: Long,
        numberOfDeployedImages: Int,
        hostServer: Server
    ) {
        outputFile.appendText("$time,$requestedBurst,$grantedBurst,$numberOfDeployedImages,$numberOfDeployedImages,${hostServer.uid}\n")
    }
}
