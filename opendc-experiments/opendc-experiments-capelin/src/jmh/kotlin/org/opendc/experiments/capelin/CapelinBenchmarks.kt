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

package org.opendc.experiments.capelin

import org.opendc.compute.service.scheduler.FilterScheduler
import org.opendc.compute.service.scheduler.filters.ComputeFilter
import org.opendc.compute.service.scheduler.filters.RamFilter
import org.opendc.compute.service.scheduler.filters.VCpuFilter
import org.opendc.compute.service.scheduler.weights.CoreRamWeigher
import org.opendc.compute.workload.*
import org.opendc.compute.workload.topology.Topology
import org.opendc.compute.workload.topology.apply
import org.opendc.experiments.capelin.topology.clusterTopology
import org.opendc.simulator.core.runBlockingSimulation
import org.openjdk.jmh.annotations.*
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit

/**
 * Benchmark suite for the Capelin experiments.
 */
@State(Scope.Thread)
@Fork(1)
@Warmup(iterations = 2, time = 5, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 5, timeUnit = TimeUnit.SECONDS)
class CapelinBenchmarks {
    private lateinit var vms: List<VirtualMachine>
    private lateinit var topology: Topology

    @Param("true", "false")
    private var isOptimized: Boolean = false

    @Setup
    fun setUp() {
        val loader = ComputeWorkloadLoader(File("src/test/resources/trace"))
        val source = trace("bitbrains-small")
        vms = source.resolve(loader, Random(1L)).vms
        topology = checkNotNull(object {}.javaClass.getResourceAsStream("/topology.txt")).use { clusterTopology(it) }
    }

    @Benchmark
    fun benchmarkCapelin() = runBlockingSimulation {
        val computeScheduler = FilterScheduler(
            filters = listOf(ComputeFilter(), VCpuFilter(16.0), RamFilter(1.0)),
            weighers = listOf(CoreRamWeigher(multiplier = 1.0))
        )
        val runner = ComputeServiceHelper(
            coroutineContext,
            clock,
            computeScheduler
        )

        try {
            runner.apply(topology, isOptimized)
            runner.run(vms, 0)
        } finally {
            runner.close()
        }
    }
}
