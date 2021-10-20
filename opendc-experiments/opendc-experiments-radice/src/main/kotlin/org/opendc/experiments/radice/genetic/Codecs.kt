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

@file:JvmName("Codecs")
package org.opendc.experiments.radice.genetic

import io.jenetics.DoubleChromosome
import io.jenetics.DoubleGene
import io.jenetics.Genotype
import io.jenetics.engine.Codec
import io.jenetics.util.IntRange
import org.opendc.experiments.radice.scenario.SchedulerSpec
import org.opendc.experiments.radice.scenario.topology.ClusterSpec
import org.opendc.experiments.radice.scenario.topology.MachineSpec
import org.opendc.experiments.radice.scenario.topology.TopologySpec
import java.math.RoundingMode
import kotlin.math.roundToInt

/**
 * Construct a [Codec] for representing a [TopologySpec] as [DoubleGene]s.
 *
 * @param clusterCount The range for the number of clusters to consider.
 * @param hostCount The range for the number of hosts in a cluster to consider.
 * @param machineModels The machine models to evaluate.
 */
fun createTopologyCodec(clusterCount: IntRange, hostCount: IntRange, machineModels: List<MachineSpec>): Codec<TopologySpec, DoubleGene> {
    val genotype = Genotype.of(
        DoubleChromosome.of(hostCount.min().toDouble(), hostCount.max().toDouble(), clusterCount), // Cluster size
        DoubleChromosome.of(0.0, (machineModels.size - 1).toDouble(), clusterCount.max()), // Machine model
    )

    return Codec.of(genotype) { gt ->
        val clusterSizes = gt[0]
        val machineModelIdx = gt[1]

        val clusters = clusterSizes.mapIndexed { i, g ->
            val idx = machineModelIdx[i].doubleValue().roundToInt()
            val machineModel = machineModels[idx]
            val hc = g.doubleValue().roundToInt()
            ClusterSpec("cluster-$i", "cluster-$i", machineModel, hc)
        }

        TopologySpec("gt", clusters)
    }
}

/**
 * Construct a [Codec] for representing a [SchedulerSpec] as [DoubleGene]s.
 *
 * @param subsetSize The range of subset sizes to consider.
 */
fun createSchedulerCodec(subsetSize: IntRange): Codec<SchedulerSpec, DoubleGene> {
    val numWeights = 5

    val genotype = Genotype.of(
        DoubleChromosome.of(-1.0, 1.0, numWeights), // Scheduler weights
        DoubleChromosome.of(subsetSize.min().toDouble(), subsetSize.max().toDouble(), 1), // Subset size
        DoubleChromosome.of(1.0, 48.0, 1), // vCPU overcommit
        DoubleChromosome.of(0.0, 1.0, 1) // Filter capacity
    )

    return Codec.of(genotype) { gt ->
        val weights = gt[0]
        SchedulerSpec(
            filterCapacity = gt[3].gene().doubleValue() > 0.5,
            ramWeight = weights[0].doubleValue().roundToScale(),
            coreRamWeight = weights[1].doubleValue().roundToScale(),
            instanceCountWeight = weights[2].doubleValue().roundToScale(),
            vcpuWeight = weights[3].doubleValue().roundToScale(),
            vcpuCapacityWeight = weights[4].doubleValue().roundToScale(),
            cpuAllocationRatio = gt[2].gene().doubleValue().roundToScale(),
            subsetSize = gt[1].gene().doubleValue().roundToInt()
        )
    }
}

/**
 * Round the specified [Double] to [scale].
 */
private fun Double.roundToScale(scale: Int = 2): Double {
    return toBigDecimal().setScale(scale, RoundingMode.HALF_UP).toDouble()
}
