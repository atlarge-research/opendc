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

package org.opendc.experiments.radice.comparison.engine

import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.filters.RamFilter
import org.opendc.compute.service.scheduler.filters.VCpuFilter
import org.opendc.compute.service.scheduler.weights.VCpuWeigher
import org.opendc.compute.workload.ComputeServiceHelper
import org.opendc.compute.workload.VirtualMachine
import org.opendc.compute.workload.topology.apply
import org.opendc.experiments.radice.scenario.topology.TopologySpec
import org.opendc.experiments.radice.scenario.topology.toTopology
import org.opendc.simulator.core.runBlockingSimulation
import org.opendc.telemetry.compute.ComputeMetricAggregator
import org.opendc.telemetry.compute.ComputeMonitor
import org.opendc.telemetry.compute.table.HostTableReader
import java.util.*

/**
 * An [ExperimentEngine] implementation using OpenDC.
 */
class OpenDCEngine : ExperimentEngine {
    override val id: String = "opendc"

    override fun runScenario(workload: List<VirtualMachine>, topology: TopologySpec, seed: Long): ExperimentResult {
        val random = Random(seed)
        val exporter = object : ComputeMonitor, ExperimentResult {
            override val powerConsumption: MutableMap<String, Double> = mutableMapOf()

            override fun record(reader: HostTableReader) {
                powerConsumption.merge(reader.host.id, reader.powerTotal, Double::plus)
            }
        }

        runBlockingSimulation {
            val scheduler = FilterScheduler(
                filters = listOf(ComputeFilter(), VCpuFilter(16.0), RamFilter(1.0)),
                weighers = listOf(VCpuWeigher(allocationRatio = 1.0, multiplier = 1.0)),
                subsetSize = 1,
                random = random
            )

            val runner = ComputeServiceHelper(
                coroutineContext,
                clock,
                scheduler,
            )

            try {
                runner.apply(topology.toTopology(random), optimize = true)
                runner.run(workload, random.nextLong(), submitImmediately = true)
            } finally {
                runner.close()
            }

            val agg = ComputeMetricAggregator()
            for (producer in runner.producers) {
                agg.process(producer.collectAllMetrics())
            }
            agg.collect(exporter)
        }

        return exporter
    }
}
