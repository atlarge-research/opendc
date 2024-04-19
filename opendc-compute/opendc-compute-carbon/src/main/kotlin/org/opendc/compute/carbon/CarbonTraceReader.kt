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

<<<<<<<< HEAD:opendc-compute/opendc-compute-carbon/src/main/kotlin/org/opendc/compute/carbon/CarbonTraceReader.kt
@file:JvmName("ComputeWorkloads")

package org.opendc.compute.carbon

import java.io.File
import javax.management.InvalidAttributeValueException

/**
 * Construct a workload from a trace.
 */
public fun getCarbonTrace(pathToFile: String?): CarbonTrace {
    if (pathToFile == null) {
        return CarbonTrace(null)
    }

    return getCarbonTrace(File(pathToFile))
}

/**
 * Construct a workload from a trace.
 */
public fun getCarbonTrace(file: File): CarbonTrace {
    if (!file.exists()) {
        throw InvalidAttributeValueException("The carbon trace cannot be found")
    }

    val fragments = CarbonTraceLoader().get(file)

    return CarbonTrace(fragments)
}
========
package org.opendc.experiments.metamodel.topology

/**
 * Definition of a compute cluster modeled in the simulation.
 *
 * @param id A unique identifier representing the compute cluster.
 * @param name The name of the cluster.
 * @param cpuCount The total number of CPUs in the cluster.
 * @param cpuSpeed The speed of a CPU in the cluster in MHz.
 * @param memCapacity The total memory capacity of the cluster (in MiB).
 * @param hostCount The number of hosts in the cluster.
 * @param memCapacityPerHost The memory capacity per host in the cluster (MiB).
 * @param cpuCountPerHost The number of CPUs per host in the cluster.
 */
public data class ClusterSpec(
    val id: String,
    val name: String,
    val cpuCount: Int,
    val cpuSpeed: Double,
    val memCapacity: Double,
    val hostCount: Int,
    val memCapacityPerHost: Double,
    val cpuCountPerHost: Int
)
>>>>>>>> e38c249e (code runnable):opendc-experiments/opendc-experiments-metamodel/src/main/kotlin/org.opendc.experiments.metamodel/opendc/experiments/metamodel/topology/ClusterSpec.kt
