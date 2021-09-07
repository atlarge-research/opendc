/*
 * Copyright (c) 2021 AtLarge Research
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

@file:JvmName("TopologyFactories")
package org.opendc.experiments.radice.scenario.topology

import org.opendc.compute.workload.topology.HostSpec
import org.opendc.compute.workload.topology.Topology
import org.opendc.simulator.compute.model.MachineModel
import org.opendc.simulator.compute.model.MemoryUnit
import org.opendc.simulator.compute.model.ProcessingNode
import org.opendc.simulator.compute.model.ProcessingUnit
import org.opendc.simulator.compute.power.SimplePowerDriver
import java.util.*
import kotlin.math.roundToInt
import kotlin.math.roundToLong

/**
 * Construct a [Topology] from a [TopologySpec].
 */
fun TopologySpec.toTopology(random: Random = Random(0)): Topology {
    return object : Topology {
        override fun resolve(): List<HostSpec> {
            val hosts = mutableListOf<HostSpec>()
            for (cluster in clusters) {
                val clusterMachineModel = cluster.machineModel
                val cpuCount = clusterMachineModel.cpuCount
                val cpuSpeed = clusterMachineModel.cpuCapacity * 1000.0 // GHz -> MHz
                val memoryPerHost = (clusterMachineModel.memCapacity * 1000.0).roundToLong() // GiB -> MiB

                val unknownProcessingNode = ProcessingNode("unknown", "unknown", "unknown", cpuCount)
                val unknownMemoryUnit = MemoryUnit("unknown", "unknown", -1.0, memoryPerHost)
                val machineModel = MachineModel(
                    List(cpuCount) { coreId -> ProcessingUnit(unknownProcessingNode, coreId, cpuSpeed) },
                    listOf(unknownMemoryUnit)
                )

                repeat(cluster.hostCount) {
                    val hostSpec = HostSpec(
                        UUID(random.nextLong(), it.toLong()),
                        "node-${cluster.name}-$it",
                        mapOf("cluster" to cluster.id),
                        machineModel,
                        SimplePowerDriver(clusterMachineModel.powerModel)
                    )

                    hosts += hostSpec
                }
            }

            return hosts
        }

        override fun toString(): String = "RadiceTopology"
    }
}

/**
 * Scale all clusters in the specified topology.
 */
fun TopologySpec.scale(id: String, factor: Double): TopologySpec {
    return TopologySpec(id, clusters.map { cluster -> cluster.copy(hostCount = (cluster.hostCount * factor).roundToInt()) })
}
